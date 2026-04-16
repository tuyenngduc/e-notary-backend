package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.CreateVideoSessionRequest;
import com.actvn.enotary.dto.request.JoinVideoSessionRequest;
import com.actvn.enotary.dto.response.VideoSessionResponse;
import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.entity.VideoSession;
import com.actvn.enotary.enums.AppointmentStatus;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.enums.VideoSessionStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.AppointmentRepository;
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

    @Value("${app.meeting.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Tạo video session cho một appointment (chỉ dành cho ONLINE service type)
     */
    @Transactional
    public VideoSessionResponse createVideoSession(CreateVideoSessionRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new AppException("Không tìm thấy lịch hẹn", HttpStatus.NOT_FOUND));

        // Kiểm tra appointment có phải ONLINE không
        if (appointment.getRequest() == null || appointment.getRequest().getServiceType() != ServiceType.ONLINE) {
            throw new AppException("Video session chỉ dành cho cuộc hẹn ONLINE", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra appointment đã có video session chưa
        if (videoSessionRepository.existsByAppointmentAppointmentId(request.getAppointmentId())) {
            throw new AppException("Lịch hẹn này đã có video session", HttpStatus.CONFLICT);
        }

        // Tạo video session
        VideoSession session = new VideoSession();
        session.setAppointment(appointment);

        // Tạo các ID duy nhất cho session
        String roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        String sessionToken = UUID.randomUUID().toString();

        session.setRoomId(roomId);
        session.setSessionToken(sessionToken);

        // Tạo meeting URL (có thể sử dụng token để access)
        String meetingUrl = baseUrl + "/api/video/room/" + roomId + "?token=" + sessionToken;
        session.setMeetingUrl(meetingUrl);

        session.setStatus(VideoSessionStatus.PENDING);
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());

        VideoSession saved = videoSessionRepository.save(session);

        // Cập nhật meeting URL trong appointment
        appointment.setMeetingUrl(meetingUrl);
        appointmentRepository.save(appointment);

        return VideoSessionResponse.fromEntity(saved);
    }

    /**
     * Lấy video session bằng appointment ID
     */
    public VideoSessionResponse getVideoSessionByAppointmentId(UUID appointmentId) {
        VideoSession session = videoSessionRepository.findByAppointmentAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND));

        return VideoSessionResponse.fromEntity(session);
    }

    /**
     * Lấy video session bằng session ID
     */
    public VideoSessionResponse getVideoSession(UUID sessionId) {
        VideoSession session = videoSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException("Không tìm thấy video session", HttpStatus.NOT_FOUND));

        return VideoSessionResponse.fromEntity(session);
    }

    /**
     * Xác minh token và trả về thông tin session
     */
    public VideoSessionResponse verifySessionToken(String token) {
        VideoSession session = videoSessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new AppException("Token không hợp lệ", HttpStatus.UNAUTHORIZED));

        return VideoSessionResponse.fromEntity(session);
    }

    /**
     * Xử lý khi user join vào video session
     * Truyền vào email để biết user là client hay notary
     */
    @Transactional
    public VideoSessionResponse joinSession(String roomId, String userEmail, String sessionToken) {
        log.info("[VideoSession] User {} attempting to join room {}", userEmail, roomId);
        VideoSession session = validateParticipantAccess(roomId, sessionToken, userEmail);
        Appointment appointment = session.getAppointment();
        boolean isNotary = appointment.getRequest().getNotary() != null
                && appointment.getRequest().getNotary().getEmail().equals(userEmail);
        boolean isClient = appointment.getRequest().getClient() != null
                && appointment.getRequest().getClient().getEmail().equals(userEmail);

        log.debug("[VideoSession] Join confirmed: isNotary={}, isClient={}", isNotary, isClient);

        // Cập nhật trạng thái join
        OffsetDateTime now = OffsetDateTime.now();
        if (isNotary && session.getNotaryJoinedAt() == null) {
            session.setNotaryJoinedAt(now);
            log.info("[VideoSession] Notary joined at {}", now);
        } else if (isClient && session.getClientJoinedAt() == null) {
            session.setClientJoinedAt(now);
            log.info("[VideoSession] Client joined at {}", now);
        }

        // Cập nhật session status
        if (session.getNotaryJoinedAt() != null && session.getClientJoinedAt() != null) {
            session.setStatus(VideoSessionStatus.IN_PROGRESS);
            log.info("[VideoSession] Both participants joined, session IN_PROGRESS");
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
                && appointment.getRequest().getNotary().getEmail().equals(userEmail);
        boolean isClient = appointment.getRequest().getClient() != null
                && appointment.getRequest().getClient().getEmail().equals(userEmail);

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

    /**
     * Kết thúc video session
     */
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

        // Tính thời gian kết nối
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
        return VideoSessionResponse.fromEntity(updated);
    }

    /**
     * Hủy video session
     */
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
        return VideoSessionResponse.fromEntity(updated);
    }
}
