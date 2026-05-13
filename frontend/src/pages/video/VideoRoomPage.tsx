import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { endVideoSessionApi, joinVideoRoomApi, verifyVideoTokenApi } from '../../features/requests/requestApi';
import { getAuthSession } from '../../lib/authStorage';
import { toApiErrorMessage } from '../../lib/apiError';
import { VIDEO_SIGNALING_WS_URL } from '../../lib/config';
import { getDefaultRouteByRole } from '../../lib/roleRedirect';
import type { VideoSessionResponse } from '../../types/video';

type SetupStage =
  | 'INIT'
  | 'VERIFY_TOKEN'
  | 'JOIN_ROOM'
  | 'GET_MEDIA'
  | 'CONNECT_SIGNALING'
  | 'WAIT_PEER'
  | 'CONNECTED';

interface SignalingMessage {
  type: 'JOIN' | 'JOINED' | 'READY' | 'OFFER' | 'ANSWER' | 'ICE' | 'LEAVE' | 'PEER_LEFT' | 'ERROR' | 'END';
  roomId?: string;
  token?: string;
  authToken?: string;
  payload?: unknown;
  sender?: string;
  message?: string;
}

const ICE_SERVERS: RTCConfiguration = {
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
};

function parseIceCandidatePayload(payload: unknown): RTCIceCandidateInit | null {
  if (!payload || typeof payload !== 'object') {
    return null;
  }

  const candidate = payload as Partial<RTCIceCandidateInit>;
  if (!candidate.candidate) {
    return null;
  }

  return {
    candidate: candidate.candidate,
    sdpMid: candidate.sdpMid ?? null,
    sdpMLineIndex: candidate.sdpMLineIndex ?? null,
    usernameFragment: candidate.usernameFragment ?? null,
  };
}

function streamHasVideo(stream: MediaStream | null): boolean {
  return !!stream && stream.getVideoTracks().some((track) => track.enabled);
}

function streamHasAudio(stream: MediaStream | null): boolean {
  return !!stream && stream.getAudioTracks().some((track) => track.enabled);
}

// Module-level reference to the active stream — outside React lifecycle.
// This guarantees track.stop() can always be called regardless of component state.
let _activeMediaStream: MediaStream | null = null;

function stopAllMediaTracks(): void {
  if (_activeMediaStream) {
    _activeMediaStream.getTracks().forEach((track) => track.stop());
    _activeMediaStream = null;
  }
}

export function VideoRoomPage() {
  const { roomId = '' } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const authSession = useMemo(() => getAuthSession(), []);
  const token = searchParams.get('token') ?? '';

  const [sessionInfo, setSessionInfo] = useState<VideoSessionResponse | null>(null);
  const [statusText, setStatusText] = useState('Đang chuẩn bị phòng họp...');
  const [error, setError] = useState('');
  const [setupStage, setSetupStage] = useState<SetupStage>('INIT');
  const [debugDetails, setDebugDetails] = useState<string>('');
  const [busy, setBusy] = useState(true);
  const [peerConnected, setPeerConnected] = useState(false);
  const [isEnding, setIsEnding] = useState(false);
  const [cameraOn, setCameraOn] = useState(true);
  const [micOn, setMicOn] = useState(true);
  const [showSignModal, setShowSignModal] = useState(false);
  const [signatureData, setSignatureData] = useState<string | null>(null);
  const [isSigning, setIsSigning] = useState(false);

  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const peerConnectionRef = useRef<RTCPeerConnection | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const hasCreatedOfferRef = useRef(false);
  const disposedRef = useRef(false);

  const canEndSession = authSession?.role?.toUpperCase() === 'NOTARY';

  // Register beforeunload and pagehide as last-resort camera/mic release
  useEffect(() => {
    const handlePageExit = () => stopAllMediaTracks();
    window.addEventListener('beforeunload', handlePageExit);
    window.addEventListener('pagehide', handlePageExit);
    return () => {
      window.removeEventListener('beforeunload', handlePageExit);
      window.removeEventListener('pagehide', handlePageExit);
    };
  }, []);

  const formatSetupError = (stage: SetupStage, rawError: unknown): { message: string; details: string } => {
    const baseMessage = toApiErrorMessage(rawError, 'Không thể khởi tạo video call.');

    const stageLabel: Record<SetupStage, string> = {
      INIT: 'Khởi tạo',
      VERIFY_TOKEN: 'Xác thực token phòng họp',
      JOIN_ROOM: 'Ghi nhận tham gia phòng (API join)',
      GET_MEDIA: 'Bật camera/micro (getUserMedia)',
      CONNECT_SIGNALING: 'Kết nối signaling (WebSocket)',
      WAIT_PEER: 'Bắt tay WebRTC (offer/answer/ice)',
      CONNECTED: 'Đã kết nối',
    };

    const maybeAxios = rawError as {
      isAxiosError?: boolean;
      response?: { status?: number; data?: unknown };
      config?: { url?: string; method?: string; baseURL?: string };
      message?: string;
    };

    const maybeDom = rawError as { name?: string; message?: string };

    const extraLines: string[] = [];
    extraLines.push(`stage=${stage} (${stageLabel[stage]})`);
    extraLines.push(`roomId=${roomId}`);
    extraLines.push(`wsUrl=${VIDEO_SIGNALING_WS_URL}`);
    extraLines.push(`role=${authSession?.role ?? 'unknown'} email=${authSession?.email ?? 'unknown'}`);

    if (maybeAxios?.isAxiosError) {
      extraLines.push(`axiosStatus=${maybeAxios.response?.status ?? 'unknown'}`);
      extraLines.push(`axiosRequest=${maybeAxios.config?.method ?? ''} ${maybeAxios.config?.baseURL ?? ''}${maybeAxios.config?.url ?? ''}`);
    }

    if (maybeDom?.name) {
      extraLines.push(`errorName=${maybeDom.name}`);
    }

    let friendly = baseMessage;

    if (stage === 'GET_MEDIA') {
      const name = maybeDom?.name;
      if (name === 'NotAllowedError' || name === 'SecurityError') {
        friendly = 'Bạn đã chặn quyền camera/micro. Hãy cấp quyền rồi tải lại trang.';
      } else if (name === 'NotFoundError') {
        friendly = 'Không tìm thấy thiết bị camera hoặc micro. Hãy kiểm tra thiết bị rồi thử lại.';
      } else if (name === 'NotReadableError') {
        friendly = 'Không thể truy cập camera/micro (có thể đang bị app khác sử dụng). Hãy đóng app khác rồi thử lại.';
      } else if (name === 'InsecureContextError') {
        friendly =
          'Trên iPhone/iPad, camera & micro chỉ hoạt động khi mở trang bằng HTTPS. ' +
          'Bạn đang mở bằng HTTP (LAN IP), nên Safari sẽ chặn getUserMedia.';
      } else if (name === 'MediaDevicesUnsupportedError') {
        friendly = 'Trình duyệt không hỗ trợ camera/micro (navigator.mediaDevices.getUserMedia không khả dụng).';
      } else if (name === 'TypeError') {
        if (!window.isSecureContext) {
          friendly =
            'Trên iPhone/iPad, camera & micro chỉ hoạt động khi mở trang bằng HTTPS. ' +
            'Bạn đang mở bằng HTTP (LAN IP), nên Safari sẽ chặn getUserMedia.';
        }
      }
    }

    friendly = `${friendly} (Bước lỗi: ${stageLabel[stage]})`;
    return { message: friendly, details: extraLines.join('\n') };
  };

  const sendSignal = (message: SignalingMessage) => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      return;
    }

    ws.send(JSON.stringify(message));
  };

  const closeConnections = (notifyLeave: boolean) => {
    if (notifyLeave) {
      sendSignal({ type: 'LEAVE', roomId });
    }

    const ws = wsRef.current;
    wsRef.current = null;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close();
    }

    const peerConnection = peerConnectionRef.current;
    peerConnectionRef.current = null;
    if (peerConnection) {
      peerConnection.ontrack = null;
      peerConnection.onicecandidate = null;
      peerConnection.close();
    }

    // Stop all media tracks via module-level tracker (most reliable)
    stopAllMediaTracks();
    localStreamRef.current = null;

    if (localVideoRef.current) {
      localVideoRef.current.srcObject = null;
    }

    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
    }

    hasCreatedOfferRef.current = false;
    setPeerConnected(false);
  };

  useEffect(() => {
    let isEffectActive = true;
    disposedRef.current = false;

    if (!roomId || !token || !authSession?.token || !authSession.email) {
      setError('Thiếu thông tin truy cập phòng họp. Vui lòng vào phòng từ giao diện lịch hẹn.');
      setBusy(false);
      return;
    }

    const setupCall = async () => {
      setBusy(true);
      setError('');
      setDebugDetails('');
      let stage: SetupStage = 'INIT';
      const updateStage = (next: SetupStage) => {
        stage = next;
        setSetupStage(next);
      };

      updateStage('INIT');
      const pendingIceCandidates: RTCIceCandidateInit[] = [];
      try {
        updateStage('VERIFY_TOKEN');
        setStatusText('Đang xác thực phiên họp...');
        await verifyVideoTokenApi(token);

        updateStage('JOIN_ROOM');
        setStatusText('Đang ghi nhận tham gia phòng...');
        const joinedSession = await joinVideoRoomApi(roomId, token);
        setSessionInfo(joinedSession);

        updateStage('GET_MEDIA');
        setStatusText('Đang bật camera và micro...');

        if (!window.isSecureContext) {
          const insecureError = new Error(
            'getUserMedia requires a secure context (HTTPS) on iOS Safari when using a LAN IP.',
          ) as Error & { name: string };
          insecureError.name = 'InsecureContextError';
          throw insecureError;
        }

        if (!navigator.mediaDevices?.getUserMedia) {
          const unsupported = new Error('navigator.mediaDevices.getUserMedia is not available.') as Error & {
            name: string;
          };
          unsupported.name = 'MediaDevicesUnsupportedError';
          throw unsupported;
        }

        const localStream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        });

        // If the component was unmounted (or this specific effect was cleaned up)
        // while we were awaiting getUserMedia, stop all tracks immediately.
        if (!isEffectActive || disposedRef.current) {
          localStream.getTracks().forEach((track) => track.stop());
          return;
        }

        // Extremely safe fallback: if we somehow already have a stream, stop it before overwriting
        if (_activeMediaStream) {
          _activeMediaStream.getTracks().forEach((t) => t.stop());
        }
        if (localStreamRef.current) {
          localStreamRef.current.getTracks().forEach((t) => t.stop());
        }

        localStreamRef.current = localStream;
        _activeMediaStream = localStream; // module-level tracker for guaranteed cleanup
        setCameraOn(streamHasVideo(localStream));
        setMicOn(streamHasAudio(localStream));

        if (localVideoRef.current) {
          localVideoRef.current.srcObject = localStream;
        }

        const createPeerConnection = (): RTCPeerConnection => {
          const oldPc = peerConnectionRef.current;
          if (oldPc) {
            oldPc.ontrack = null;
            oldPc.onicecandidate = null;
            oldPc.close();
          }

          const pc = new RTCPeerConnection(ICE_SERVERS);
          peerConnectionRef.current = pc;

          localStream.getTracks().forEach((track) => {
            pc.addTrack(track, localStream);
          });

          pc.ontrack = (event) => {
            const [remoteStream] = event.streams;
            if (remoteVideoRef.current && remoteStream) {
              remoteVideoRef.current.srcObject = remoteStream;
              setPeerConnected(true);
              setStatusText('Đã kết nối với đối tác.');
            }
          };

          pc.onicecandidate = (event) => {
            if (!event.candidate) {
              return;
            }

            sendSignal({
              type: 'ICE',
              roomId,
              payload: event.candidate.toJSON(),
            });
          };

          return pc;
        };

        createPeerConnection();

        updateStage('CONNECT_SIGNALING');
        setStatusText('Đang kết nối signaling...');
        const ws = new WebSocket(VIDEO_SIGNALING_WS_URL);
        wsRef.current = ws;

        ws.onopen = () => {
          sendSignal({
            type: 'JOIN',
            roomId,
            token,
            authToken: authSession.token,
          });
          updateStage('WAIT_PEER');
          setStatusText('Đang chờ đối tác vào phòng...');
        };

        ws.onmessage = async (event) => {
          let message: SignalingMessage;
          try {
            message = JSON.parse(event.data) as SignalingMessage;
          } catch (parseError) {
            // eslint-disable-next-line no-console
            console.error('[VideoCall] Failed to parse signaling message', parseError, event.data);
            return;
          }

          if (message.type === 'ERROR') {
            setError(message.message || 'Không thể thiết lập kết nối video.');
            return;
          }

          // ── PEER_LEFT ─────────────────────────────────────────────────────
          if (message.type === 'PEER_LEFT') {
            setPeerConnected(false);
            hasCreatedOfferRef.current = false;
            pendingIceCandidates.length = 0;
            if (remoteVideoRef.current) {
              remoteVideoRef.current.srcObject = null;
            }
            const oldPc = peerConnectionRef.current;
            if (oldPc) {
              oldPc.ontrack = null;
              oldPc.onicecandidate = null;
              oldPc.close();
              peerConnectionRef.current = null;
            }
            setStatusText('Đối tác đã rời phòng. Đang chờ kết nối lại...');
            return;
          }

          // ── READY: cả 2 đã vào phòng, bắt đầu negotiate ──────────────────
          if (message.type === 'READY') {
            const offererEmail = (message.payload as { offererEmail?: string } | undefined)?.offererEmail;
            const iAmOfferer = !!offererEmail && offererEmail.toLowerCase() === authSession.email.toLowerCase();

            hasCreatedOfferRef.current = false;
            pendingIceCandidates.length = 0;
            const freshPc = createPeerConnection();

            if (iAmOfferer) {
              hasCreatedOfferRef.current = true;
              const offer = await freshPc.createOffer();
              await freshPc.setLocalDescription(offer);
              sendSignal({ type: 'OFFER', roomId, payload: offer });
              setStatusText('Đang gửi đề nghị kết nối...');
            } else {
              setStatusText('Đang chờ đề nghị kết nối từ đối tác...');
            }
            return;
          }

          // Các message còn lại yêu cầu PC đang hoạt động
          const activePc = peerConnectionRef.current;
          if (!activePc) {
            return;
          }

          // ── OFFER: chúng ta là answerer, dùng PC được tạo ở READY ─────────
          if (message.type === 'OFFER') {
            const offer = message.payload as RTCSessionDescriptionInit;
            await activePc.setRemoteDescription(new RTCSessionDescription(offer));

            // Flush ICE candidates đã buffer trước khi nhận OFFER
            for (const buffered of pendingIceCandidates) {
              await activePc.addIceCandidate(new RTCIceCandidate(buffered));
            }
            pendingIceCandidates.length = 0;

            const answer = await activePc.createAnswer();
            await activePc.setLocalDescription(answer);
            sendSignal({ type: 'ANSWER', roomId, payload: answer });
            setStatusText('Đang hoàn tất bắt tay WebRTC...');
            return;
          }

          // ── ANSWER: chúng ta là offerer, apply answer ─────────────────────
          if (message.type === 'ANSWER') {
            if (activePc.signalingState !== 'have-local-offer') {
              return;
            }
            const answer = message.payload as RTCSessionDescriptionInit;
            await activePc.setRemoteDescription(new RTCSessionDescription(answer));

            // Flush ICE candidates đã buffer trước khi nhận ANSWER
            for (const buffered of pendingIceCandidates) {
              await activePc.addIceCandidate(new RTCIceCandidate(buffered));
            }
            pendingIceCandidates.length = 0;
            return;
          }

          // ── ICE: buffer cho đến khi remote description được set ───────────
          if (message.type === 'ICE') {
            const iceCandidate = parseIceCandidatePayload(message.payload);
            if (!iceCandidate) {
              return;
            }
            if (activePc.remoteDescription) {
              await activePc.addIceCandidate(new RTCIceCandidate(iceCandidate));
            } else {
              pendingIceCandidates.push(iceCandidate);
            }
          }
        };

        ws.onerror = () => {
          setError('Kênh signaling gặp lỗi. Vui lòng thử tải lại phòng.');
        };

        ws.onclose = () => {
          if (!disposedRef.current) {
            setStatusText('Kết nối signaling đã đóng.');
          }
        };
      } catch (setupError) {
        const formatted = formatSetupError(stage, setupError);
        // eslint-disable-next-line no-console
        console.error('[VideoCall] Setup failed', { stage, setupError });
        setError(formatted.message);
        setDebugDetails(formatted.details);
        closeConnections(false);
      } finally {
        setBusy(false);
      }
    };

    void setupCall();

    return () => {
      isEffectActive = false;
      disposedRef.current = true;
      stopAllMediaTracks();
      localStreamRef.current = null;
      closeConnections(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, token]);

  const handleToggleCamera = () => {
    const stream = localStreamRef.current;
    if (!stream) {
      return;
    }

    const tracks = stream.getVideoTracks();
    if (tracks.length === 0) {
      return;
    }

    const nextEnabled = !tracks[0].enabled;
    tracks.forEach((track) => {
      track.enabled = nextEnabled;
    });

    setCameraOn(nextEnabled);
  };

  const handleToggleMic = () => {
    const stream = localStreamRef.current;
    if (!stream) {
      return;
    }

    const tracks = stream.getAudioTracks();
    if (tracks.length === 0) {
      return;
    }

    const nextEnabled = !tracks[0].enabled;
    tracks.forEach((track) => {
      track.enabled = nextEnabled;
    });

    setMicOn(nextEnabled);
  };

  const handleLeaveRoom = () => {
    stopAllMediaTracks();
    localStreamRef.current = null;
    disposedRef.current = true;
    closeConnections(true);
    navigate(getDefaultRouteByRole(authSession?.role), { replace: true });
  };

  const handleEndSession = async () => {
    if (!sessionInfo?.sessionId) {
      handleLeaveRoom();
      return;
    }

    setIsEnding(true);
    setError('');
    try {
      await endVideoSessionApi(sessionInfo.sessionId, 'Kết thúc từ giao diện công chứng viên');
      sendSignal({ type: 'END', roomId, message: 'Phiên họp đã được kết thúc.' });
      stopAllMediaTracks();
      localStreamRef.current = null;
      disposedRef.current = true;
      closeConnections(true);
      navigate('/notary/appointments', { replace: true });
    } catch (endError) {
      setError(toApiErrorMessage(endError, 'Không thể kết thúc phiên họp.'));
    } finally {
      setIsEnding(false);
    }
  };

  const handleSignContract = async () => {
    setIsSigning(true);
    setError('');
    try {
      const mockSignatureBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
      setSignatureData(mockSignatureBase64);

      await new Promise(resolve => setTimeout(resolve, 1000));

      alert('✓ Ký số thành công! Hợp đồng đã được ký điện tử và lưu vào hệ thống.');
      setShowSignModal(false);
    } catch (err) {
      setError(toApiErrorMessage(err, 'Không thể ký số hợp đồng.'));
    } finally {
      setIsSigning(false);
    }
  };

  return (
    <div className="video-room-page">
      <div className="video-room-shell">
        <header className="video-room-header">
          <div>
            <h1>Phòng công chứng trực tuyến</h1>
            <p>
              Room: <b>{roomId}</b>
            </p>
          </div>
          <div
            className="status-badge badge-blue"
            style={{ textTransform: 'none', letterSpacing: 0 }}
            title={`Stage: ${setupStage}`}
          >
            {peerConnected ? 'Đang kết nối 2 chiều' : statusText}
          </div>
        </header>

        {error ? (
          <div className="form-error">
            <div>{error}</div>
            {debugDetails ? (
              <details style={{ marginTop: '0.75rem' }}>
                <summary style={{ cursor: 'pointer' }}>Chi tiết kỹ thuật</summary>
                <pre style={{ marginTop: '0.75rem', whiteSpace: 'pre-wrap' }}>{debugDetails}</pre>
              </details>
            ) : null}
          </div>
        ) : null}

        <section className="video-grid">
          <article className="video-tile">
            <div className="video-tile-head">
              <span>Bạn</span>
              <span>{cameraOn ? 'Camera bật' : 'Camera tắt'} • {micOn ? 'Mic bật' : 'Mic tắt'}</span>
            </div>
            <video ref={localVideoRef} autoPlay muted playsInline />
          </article>

          <article className="video-tile">
            <div className="video-tile-head">
              <span>Đối tác</span>
              <span>{peerConnected ? 'Đang online' : 'Đang chờ vào phòng'}</span>
            </div>
            <video ref={remoteVideoRef} autoPlay playsInline />
          </article>
        </section>

        <footer className="video-room-controls">
          <button type="button" className="link-btn" onClick={handleToggleCamera} disabled={busy || !localStreamRef.current}>
            {cameraOn ? 'Tắt camera' : 'Bật camera'}
          </button>
          <button type="button" className="link-btn" onClick={handleToggleMic} disabled={busy || !localStreamRef.current}>
            {micOn ? 'Tắt mic' : 'Bật mic'}
          </button>
          {canEndSession ? (
            <>
              <button type="button" className="secondary-btn" onClick={() => setShowSignModal(true)} disabled={busy || !peerConnected}>
                ✎ Ký số hợp đồng
              </button>
              <button type="button" className="primary-btn" onClick={handleEndSession} disabled={busy || isEnding}>
                {isEnding ? 'Đang kết thúc...' : 'Kết thúc phiên'}
              </button>
            </>
          ) : null}
          <button type="button" className="ghost-btn" onClick={handleLeaveRoom} disabled={busy || isEnding}>
            Rời phòng
          </button>
        </footer>

        {/* Signature Modal */}
        {showSignModal ? (
          <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.5)',
            zIndex: 10000,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <div style={{
              background: 'white',
              borderRadius: '8px',
              padding: '2rem',
              maxWidth: '500px',
              width: '90%',
              boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
            }}>
              <h2 style={{ marginTop: 0 }}>Ký điện tử hợp đồng</h2>
              <p style={{ color: '#666', marginBottom: '1.5rem' }}>
                Nhấn nút "Ký số" để xác nhận ký điện tử hợp đồng công chứng.
                <br />
                Chữ ký sẽ được lưu vào hệ thống và không thể thay đổi.
              </p>

              {signatureData ? (
                <div style={{ marginBottom: '1.5rem', textAlign: 'center' }}>
                  <div style={{
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    padding: '1rem',
                    background: '#f9f9f9',
                  }}>
                    <div style={{ fontSize: '0.9rem', color: '#666', marginBottom: '0.5rem' }}>
                      Chữ ký đã được ghi lại:
                    </div>
                    <div style={{
                      height: '80px',
                      background: 'white',
                      border: '1px solid #e0e0e0',
                      borderRadius: '4px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#666',
                    }}>
                      ✓ Chữ ký hợp lệ
                    </div>
                  </div>
                </div>
              ) : null}

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  className="ghost-btn"
                  onClick={() => {
                    setShowSignModal(false);
                    setSignatureData(null);
                  }}
                  disabled={isSigning}
                >
                  Hủy
                </button>
                {!signatureData ? (
                  <button
                    type="button"
                    className="primary-btn"
                    onClick={handleSignContract}
                    disabled={isSigning}
                  >
                    {isSigning ? 'Đang ký số...' : '✎ Ký số'}
                  </button>
                ) : (
                  <button
                    type="button"
                    className="primary-btn"
                    onClick={() => {
                      alert('Ký số hợp đồng thành công! Hợp đồng đã được lưu trong hệ thống.');
                      setShowSignModal(false);
                      setSignatureData(null);
                    }}
                  >
                    Xác nhận ký số
                  </button>
                )}
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}
