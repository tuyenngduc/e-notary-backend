package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.CreateVideoSessionRequest;
import com.actvn.enotary.dto.response.VideoSessionResponse;
import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.VideoSession;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.enums.VideoSessionStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.AppointmentRepository;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.VideoSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j

@Service
@RequiredArgsConstructor
public class VideoSessionService {

    private final VideoSessionRepository videoSessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotaryRequestRepository notaryRequestRepository;

    @Value("${app.meeting.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public VideoSessionResponse createVideoSession(CreateVideoSessionRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new AppException("Không tìm thấy lịch hẹn", HttpStatus.NOT_FOUND));

        if (appointment.getRequest() == null || appointment.getRequest().getServiceType() != ServiceType.ONLINE) {
            throw new AppException("Video session chỉ dành cho cuộc hẹn ONLINE", HttpStatus.BAD_REQUEST);
        }

        if (videoSessionRepository.existsByAppointmentAppointmentId(request.getAppointmentId())) {
            throw new AppException("Lịch hẹn này đã có video session", HttpStatus.CONFLICT);
        }

        VideoSession session = new VideoSession();
        session.setAppointment(appointment);

        String roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        String sessionToken = UUID.randomUUID().toString();

        session.setRoomId(roomId);
        session.setSessionToken(sessionToken);

        String meetingUrl = baseUrl + "/api/video/room/" + roomId + "?token=" + sessionToken;
        session.setMeetingUrl(meetingUrl);

        session.setStatus(VideoSessionStatus.PENDING);
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());

        VideoSession saved = videoSessionRepository.save(session);

        appointment.setMeetingUrl(meetingUrl);
        appointmentRepository.save(appointment);

        return VideoSessionResponse.fromEntity(saved);
    }

    public VideoSessionResponse getVideoSessionByAppointmentId(UUID appointmentId) {
        VideoSession session = videoSessionRepository.findByAppointmentAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND));

        return VideoSessionResponse.fromEntity(session);
    }

    public VideoSessionResponse getVideoSession(UUID sessionId) {
        VideoSession session = videoSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND));

        return VideoSessionResponse.fromEntity(session);
    }

    public VideoSessionResponse verifySessionToken(String token) {
        VideoSession session = videoSessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new AppException("Token không hợp lệ", HttpStatus.UNAUTHORIZED));

        return VideoSessionResponse.fromEntity(session);
    }

    @Transactional
    public VideoSessionResponse joinSession(String roomId, String userEmail, String sessionToken) {
        log.info("[VideoSession] User {} attempting to join room {}", userEmail, roomId);
        VideoSession session = validateParticipantAccess(roomId, sessionToken, userEmail);
        Appointment appointment = session.getAppointment();
        boolean isNotary = appointment.getRequest().getNotary() != null
                && appointment.getRequest().getNotary().getEmail() != null
                && appointment.getRequest().getNotary().getEmail().equalsIgnoreCase(userEmail);
        boolean isClient = appointment.getRequest().getClient() != null
                && appointment.getRequest().getClient().getEmail() != null
                && appointment.getRequest().getClient().getEmail().equalsIgnoreCase(userEmail);

        log.debug("[VideoSession] Join confirmed: isNotary={}, isClient={}", isNotary, isClient);

        OffsetDateTime now = OffsetDateTime.now();
        if (isNotary && session.getNotaryJoinedAt() == null) {
            session.setNotaryJoinedAt(now);
            log.info("[VideoSession] Notary joined at {}", now);
        } else if (isClient && session.getClientJoinedAt() == null) {
            session.setClientJoinedAt(now);
            log.info("[VideoSession] Client joined at {}", now);
        }

        if (session.getNotaryJoinedAt() != null && session.getClientJoinedAt() != null) {
            session.setStatus(VideoSessionStatus.IN_PROGRESS);
            log.info("[VideoSession] Both participants joined, session IN_PROGRESS");
            transitionRequestToInVideoCall(session);
        } else if (session.getStatus() == VideoSessionStatus.PENDING && isNotary) {
            session.setStatus(VideoSessionStatus.NOTARY_JOINED);
            log.info("[VideoSession] First participant (Notary) joined");
        }

        session.setUpdatedAt(now);
        VideoSession updated = videoSessionRepository.save(session);
        log.info("[VideoSession] Session {} updated with status {}", session.getSessionId(), session.getStatus());

        return VideoSessionResponse.fromEntity(updated);
    }

    public VideoSession validateParticipantAccess(String roomId, String sessionToken, String userEmail) {
        if (sessionToken == null || sessionToken.isBlank()) {
            log.warn("[VideoSession] Missing session token for room {}", roomId);
            throw new AppException("Thiếu token truy cập phòng họp", HttpStatus.UNAUTHORIZED);
        }

        VideoSession session = videoSessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> {
                    log.warn("[VideoSession] Room {} not found", roomId);
                    return new AppException("Phòng họp không tồn tại", HttpStatus.NOT_FOUND);
                });

        if (!sessionToken.equals(session.getSessionToken())) {
            log.warn("[VideoSession] Invalid token for room {}", roomId);
            throw new AppException("Token phòng họp không hợp lệ", HttpStatus.UNAUTHORIZED);
        }

        Appointment appointment = session.getAppointment();
        if (appointment == null || appointment.getRequest() == null) {
            log.error("[VideoSession] Invalid video session data for room {}", roomId);
            throw new AppException("Dữ liệu phiên video không hợp lệ", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        boolean isNotary = appointment.getRequest().getNotary() != null
                && appointment.getRequest().getNotary().getEmail() != null
                && appointment.getRequest().getNotary().getEmail().equalsIgnoreCase(userEmail);
        boolean isClient = appointment.getRequest().getClient() != null
                && appointment.getRequest().getClient().getEmail() != null
                && appointment.getRequest().getClient().getEmail().equalsIgnoreCase(userEmail);

        if (!isNotary && !isClient) {
            log.warn("[VideoSession] Unauthorized access attempt to room {} by {}", roomId, userEmail);
            throw new AppException("Bạn không có quyền truy cập phòng họp này", HttpStatus.FORBIDDEN);
        }

        if (session.getStatus() == VideoSessionStatus.FINISHED || session.getStatus() == VideoSessionStatus.CANCELLED) {
            log.warn("[VideoSession] Room {} already finished/cancelled", roomId);
            throw new AppException("Phiên video đã kết thúc hoặc đã hủy", HttpStatus.BAD_REQUEST);
        }

        log.debug("[VideoSession] Access validation passed for {} in room {}", userEmail, roomId);
        return session;
    }

    @Transactional
    public VideoSessionResponse endSession(UUID sessionId, String reason) {
        log.info("[VideoSession] Ending session {} with reason: {}", sessionId, reason);
        VideoSession session = videoSessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("[VideoSession] Session {} not found", sessionId);
                    return new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND);
                });

        OffsetDateTime now = OffsetDateTime.now();
        session.setEndedAt(now);

        if (session.getClientJoinedAt() != null) {
            long seconds = java.time.temporal.ChronoUnit.SECONDS.between(
                    session.getClientJoinedAt(),
                    now
            );
            session.setDurationSeconds(seconds);
            log.info("[VideoSession] Session {} duration: {} seconds", sessionId, seconds);
        } else if (session.getNotaryJoinedAt() != null) {
            long seconds = java.time.temporal.ChronoUnit.SECONDS.between(
                    session.getNotaryJoinedAt(),
                    now
            );
            session.setDurationSeconds(seconds);
        }

        session.setStatus(VideoSessionStatus.FINISHED);
        session.setNotes(reason);
        session.setUpdatedAt(now);

        VideoSession updated = videoSessionRepository.save(session);
        log.info("[VideoSession] Session {} marked as FINISHED", sessionId);

        transitionRequestAfterVideoCallEnded(session);

        return VideoSessionResponse.fromEntity(updated);
    }

    @Transactional
    public VideoSessionResponse cancelSession(UUID sessionId, String reason) {
        log.info("[VideoSession] Cancelling session {} with reason: {}", sessionId, reason);
        VideoSession session = videoSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND));

        session.setStatus(VideoSessionStatus.CANCELLED);
        session.setNotes(reason);
        session.setUpdatedAt(OffsetDateTime.now());

        VideoSession updated = videoSessionRepository.save(session);
        log.info("[VideoSession] Session {} marked as CANCELLED", sessionId);
        transitionRequestAfterVideoCallEnded(session);
        return VideoSessionResponse.fromEntity(updated);
    }

    private void transitionRequestToInVideoCall(VideoSession session) {
        try {
            NotaryRequest request = session.getAppointment().getRequest();
            if (request != null && request.getStatus() == RequestStatus.SCHEDULED) {
                request.setStatus(RequestStatus.IN_VIDEO_CALL);
                request.setUpdatedAt(OffsetDateTime.now());
                notaryRequestRepository.save(request);
                log.info("[VideoSession] NotaryRequest {} transitioned to IN_VIDEO_CALL", request.getRequestId());
            }
        } catch (Exception ex) {
            log.warn("[VideoSession] Could not transition request to IN_VIDEO_CALL: {}", ex.getMessage());
        }
    }

    private void transitionRequestAfterVideoCallEnded(VideoSession session) {
        try {
            NotaryRequest request = session.getAppointment().getRequest();
            if (request != null && request.getStatus() == RequestStatus.IN_VIDEO_CALL) {
                request.setStatus(RequestStatus.SCHEDULED);
                request.setUpdatedAt(OffsetDateTime.now());
                notaryRequestRepository.save(request);
                log.info("[VideoSession] NotaryRequest {} transitioned back to SCHEDULED after video call ended", request.getRequestId());
            }
        } catch (Exception ex) {
            log.warn("[VideoSession] Could not transition request after video call ended: {}", ex.getMessage());
        }
    }
}

