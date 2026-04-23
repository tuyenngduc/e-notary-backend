import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { DashboardLayout } from '../../components/DashboardLayout';
import {
  acceptRequestApi,
  downloadDocumentApi,
  getRequestApi,
  getRequestDocumentsApi,
  rejectRequestApi,
  scheduleRequestApi,
} from '../../features/requests/requestApi';
import { toApiErrorMessage } from '../../lib/apiError';
import {
  toContractTypeLabel,
  toDocTypeLabel,
  toRequestStatusMeta,
  toServiceTypeLabel,
} from '../../lib/enumLabels';
import type { DocumentItem, NotaryRequest } from '../../types/request';

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN');
}

export function NotaryRequestDetailPage() {
  const { id = '' } = useParams();
  const [request, setRequest] = useState<NotaryRequest | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [scheduleDateTime, setScheduleDateTime] = useState('');
  const [scheduleAddress, setScheduleAddress] = useState('');
  const [viewingDocUrl, setViewingDocUrl] = useState<string | null>(null);

  const statusInfo = useMemo(() => {
    if (!request) return null;
    return toRequestStatusMeta(request.status);
  }, [request]);

  const reload = async () => {
    if (!id) {
      return;
    }

    const [requestData, documentData] = await Promise.all([getRequestApi(id), getRequestDocumentsApi(id)]);
    setRequest(requestData);
    setDocuments(documentData);
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        await reload();
      } catch (loadError) {
        setError(toApiErrorMessage(loadError, 'Không tải được chi tiết yêu cầu'));
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [id]);

  const handleAccept = async () => {
    setSubmitting(true);
    setError('');
    try {
      await acceptRequestApi(id);
      await reload();
    } catch (actionError) {
      setError(toApiErrorMessage(actionError, 'Không thể tiếp nhận yêu cầu'));
    } finally {
      setSubmitting(false);
    }
  };

  const executeReject = async () => {
    if (!rejectReason.trim()) {
      setError('Vui lòng nhập lý do từ chối');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await rejectRequestApi(id, rejectReason.trim());
      setRejectReason('');
      setShowRejectModal(false);
      await reload();
    } catch (actionError) {
      setError(toApiErrorMessage(actionError, 'Không thể từ chối yêu cầu'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSchedule = async () => {
    if (!scheduleDateTime) {
      setError('Vui lòng chọn thời gian hẹn');
      return;
    }

    if (request?.serviceType === 'OFFLINE' && !scheduleAddress.trim()) {
      setError('Vui lòng nhập địa chỉ gặp mặt (dịch vụ trực tiếp)');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const isoTime = new Date(scheduleDateTime).toISOString();
      await scheduleRequestApi(id, isoTime, request?.serviceType === 'OFFLINE' ? scheduleAddress : undefined);
      await reload();
    } catch (actionError) {
      setError(toApiErrorMessage(actionError, 'Không thể lên lịch hẹn'));
    } finally {
      setSubmitting(false);
    }
  };

  const missingDocTypes = request?.documentRequirements?.missingDocTypes ?? [];
  const canAccept = request?.status === 'PROCESSING' && missingDocTypes.length === 0;
  const isTerminal = request?.status === 'COMPLETED' || request?.status === 'CANCELLED' || request?.status === 'REJECTED';

  const handleDownload = async (item: DocumentItem) => {
    try {
      const blob = await downloadDocumentApi(item.documentId);
      const objectUrl = URL.createObjectURL(blob);
      const anchor = window.document.createElement('a');
      anchor.href = objectUrl;
      anchor.download = item.filePath.split('/').pop() || `${item.documentId}.bin`;
      window.document.body.append(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch (downloadError) {
      setError(toApiErrorMessage(downloadError, 'Không tải được tài liệu'));
    }
  };

  const handleView = async (item: DocumentItem) => {
    try {
      const blob = await downloadDocumentApi(item.documentId);
      let finalBlob = blob;

      const path = item.filePath.toLowerCase();
      if (path.endsWith('.pdf') && blob.type !== 'application/pdf') {
        finalBlob = new Blob([blob], { type: 'application/pdf' });
      } else if ((path.endsWith('.jpg') || path.endsWith('.jpeg')) && blob.type !== 'image/jpeg') {
        finalBlob = new Blob([blob], { type: 'image/jpeg' });
      } else if (path.endsWith('.png') && blob.type !== 'image/png') {
        finalBlob = new Blob([blob], { type: 'image/png' });
      }

      const objectUrl = URL.createObjectURL(finalBlob);
      setViewingDocUrl(objectUrl);
    } catch (viewError) {
      setError(toApiErrorMessage(viewError, 'Không xem được tài liệu'));
    }
  };

  return (
    <DashboardLayout role="notary">
      <div className="page-content">
        {loading ? <p className="muted-text">Đang tải dữ liệu...</p> : null}
        {error ? <div className="form-error">{error}</div> : null}

        {!loading && !error && request ? (
          <>
            <div className="page-header with-action">
              <div>
                <h1>Chi tiết yêu cầu công chứng</h1>
                <p>
                  Mã YC: <b>{request.requestId.split('-')[0]}</b> · Tạo lúc {formatDate(request.createdAt)}
                </p>
              </div>
              {statusInfo ? <span className={`status-badge ${statusInfo.tone}`}>{statusInfo.label}</span> : null}
            </div>

            <div className="detail-layout">
              {/* Cột chính bên trái */}
              <div className="detail-main">
                <section className="soft-card">
                  <h2>Thông tin chung</h2>
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">Loại hợp đồng</span>
                      <span className="info-value">{toContractTypeLabel(request.contractType)}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Hình thức công chứng</span>
                      <span className="info-value">{toServiceTypeLabel(request.serviceType)}</span>
                    </div>
                    <div className="info-item" style={{ gridColumn: '1 / -1' }}>
                      <span className="info-label">Mô tả thêm</span>
                      <span className="info-value" style={{ fontWeight: 400 }}>{request.description || 'Không có mô tả'}</span>
                    </div>
                  </div>


                </section>

                <section className="soft-card">
                  <h2>Tài liệu đính kèm</h2>
                  {documents.length === 0 ? <p className="muted-text">Khách hàng chưa tải lên tài liệu nào.</p> : null}
                  <div className="list-stack" style={{ marginTop: '1rem' }}>
                    {documents.map((item) => (
                      <div className="doc-card" key={item.documentId}>
                        <div className="doc-icon-wrap">
                          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                            <line x1="16" y1="13" x2="8" y2="13"></line>
                            <line x1="16" y1="17" x2="8" y2="17"></line>
                            <polyline points="10 9 9 9 8 9"></polyline>
                          </svg>
                        </div>
                        <div className="doc-info">
                          <h3 title={item.filePath.split('/').pop()}>{item.filePath.split('/').pop()}</h3>
                          <p>{toDocTypeLabel(item.docType)}</p>
                        </div>
                        <div className="doc-actions">
                          <button type="button" className="ghost-btn" onClick={() => void handleView(item)}>Xem</button>
                          <button type="button" className="link-btn" onClick={() => void handleDownload(item)}>Tải</button>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              </div>

              {/* Cột hành động bên phải */}
              <div className="detail-sidebar">
                <div className="sticky-panel">
                  {missingDocTypes.length > 0 ? (
                    <div className="alert-box alert-warning">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                        <line x1="12" y1="9" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                      </svg>
                      <div>
                        <strong>Thiếu hồ sơ</strong>
                        <p style={{ marginTop: '0.25rem', fontSize: '0.9rem' }}>{missingDocTypes.map((t) => toDocTypeLabel(t)).join(', ')}</p>
                      </div>
                    </div>
                  ) : request.status === 'PROCESSING' ? (
                    <div className="alert-box alert-success">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                      </svg>
                      <div>
                        <strong>Đủ điều kiện</strong>
                        <p style={{ marginTop: '0.25rem', fontSize: '0.9rem' }}>Hồ sơ đã đủ giấy tờ tối thiểu.</p>
                      </div>
                    </div>
                  ) : null}

                  <section className="soft-card">
                    <h2 style={{ marginBottom: '1rem' }}>Hành động</h2>

                    {request.status === 'ACCEPTED' ? (
                      <div>
                        <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Lên lịch hẹn</h3>

                        <label className="field" style={{ marginBottom: '1rem' }}>
                          <span>Thời gian hẹn *</span>
                          <input
                            type="datetime-local"
                            value={scheduleDateTime}
                            onChange={(event) => setScheduleDateTime(event.target.value)}
                          />
                        </label>

                        {request.serviceType === 'OFFLINE' ? (
                          <label className="field" style={{ marginBottom: '1.5rem' }}>
                            <span>Địa chỉ gặp mặt *</span>
                            <input
                              type="text"
                              value={scheduleAddress}
                              onChange={(event) => setScheduleAddress(event.target.value)}
                              placeholder="Văn phòng công chứng..."
                            />
                          </label>
                        ) : null}

                        <button
                          type="button"
                          className="primary-btn w-full"
                          onClick={handleSchedule}
                          disabled={submitting}
                        >
                          {submitting ? 'Đang xử lý...' : 'Xác nhận lịch hẹn'}
                        </button>
                      </div>
                    ) : ['NEW', 'PROCESSING'].includes(request.status) ? (
                      <div className="action-row" style={{ flexDirection: 'column', gap: '0.75rem' }}>
                        <button
                          type="button"
                          className="primary-btn w-full"
                          onClick={handleAccept}
                          disabled={submitting || !canAccept}
                          title={
                            request.status !== 'PROCESSING'
                              ? 'Chỉ tiếp nhận khi ở trạng thái chờ xử lý'
                              : missingDocTypes.length > 0
                                ? 'Hồ sơ chưa đủ giấy tờ bắt buộc'
                                : undefined
                          }
                        >
                          Tiếp nhận yêu cầu
                        </button>
                        <button
                          type="button"
                          className="ghost-btn w-full"
                          style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
                          onClick={() => setShowRejectModal(true)}
                          disabled={submitting}
                        >
                          Từ chối yêu cầu
                        </button>
                        {request.status === 'NEW' && !canAccept && (
                          <p className="muted-text" style={{ fontSize: '0.85rem', textAlign: 'center', marginTop: '0.5rem' }}>
                            Vui lòng đợi khách hàng bổ sung đủ hồ sơ.
                          </p>
                        )}
                      </div>
                    ) : request.status === 'SCHEDULED' ? (
                      <div style={{ textAlign: 'center' }}>
                        <div className="alert-box alert-info" style={{ marginBottom: '1rem', textAlign: 'left' }}>
                          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                          </svg>
                          <span style={{ fontSize: '0.95rem' }}>
                            {request.serviceType === 'ONLINE'
                              ? 'Lịch hẹn trực tuyến đã được xác nhận. Vui lòng tham gia đúng giờ.'
                              : 'Lịch hẹn trực tiếp đã được xác nhận. Vui lòng gặp khách hàng tại văn phòng đúng giờ.'}
                          </span>
                        </div>
                        {request.serviceType === 'ONLINE' && request.meetingUrl ? (
                          <a href={request.meetingUrl} target="_blank" rel="noreferrer" className="primary-btn w-full" style={{ justifyContent: 'center' }}>
                            Mở phòng họp
                          </a>
                        ) : null}
                      </div>
                    ) : (
                      <p className="muted-text" style={{ fontSize: '0.95rem', textAlign: 'center', padding: '1rem 0' }}>
                        Không có hành động khả dụng.
                      </p>
                    )}
                  </section>
                </div>
              </div>
            </div>
          </>
        ) : null}

        {viewingDocUrl ? (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.8)', backdropFilter: 'blur(8px)', zIndex: 9999, display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 2rem', background: '#0f172a', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
              <span style={{ color: 'white', fontWeight: 600, fontSize: '1.1rem' }}>Trình xem tài liệu</span>
              <button className="primary-btn" style={{ background: '#ef4444', padding: '0.5rem 1.5rem', borderRadius: '8px' }} onClick={() => { URL.revokeObjectURL(viewingDocUrl); setViewingDocUrl(null); }}>
                Đóng
              </button>
            </div>
            <iframe src={viewingDocUrl} style={{ width: '100%', flex: 1, border: 'none', background: '#f8fafc' }} title="Trình xem tài liệu" />
          </div>
        ) : null}

        {showRejectModal ? (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.6)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
            <div className="soft-card" style={{ width: '100%', maxWidth: '520px', padding: '2.5rem' }}>
              <h3 style={{ margin: 0, fontSize: '1.5rem' }}>Từ chối yêu cầu</h3>
              <p className="muted-text" style={{ marginTop: '0.5rem', marginBottom: '1.5rem' }}>
                Vui lòng nhập lý do từ chối để khách hàng có thể bổ sung hoặc điều chỉnh hồ sơ.
              </p>

              <div className="field">
                <span>Lý do từ chối *</span>
                <textarea
                  rows={4}
                  value={rejectReason}
                  onChange={(event) => setRejectReason(event.target.value)}
                  placeholder="Ví dụ: Thiếu giấy tờ tùy thân, thông tin chưa khớp..."
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                <button type="button" className="ghost-btn" style={{ flex: 1 }} onClick={() => setShowRejectModal(false)} disabled={submitting}>
                  Hủy
                </button>
                <button type="button" className="primary-btn" style={{ flex: 1, background: '#ef4444' }} onClick={() => void executeReject()} disabled={submitting}>
                  {submitting ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </DashboardLayout>
  );
}

