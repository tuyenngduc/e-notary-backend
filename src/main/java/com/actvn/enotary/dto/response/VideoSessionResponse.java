package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.VideoSession;
import com.actvn.enotary.enums.VideoSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO response cho video session
 */
@Data
@Builder
public class VideoSessionResponse {
    private UUID sessionId;
    private UUID appointmentId;
    private AppointmentResponse appointment;  // NEW: Include appointment object
    private String sessionToken;
    private String meetingUrl;
    private String roomId;
    private VideoSessionStatus status;
    private OffsetDateTime notaryJoinedAt;
    private OffsetDateTime clientJoinedAt;
    private OffsetDateTime endedAt;
    private Long durationSeconds;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static VideoSessionResponse fromEntity(VideoSession session) {
        return VideoSessionResponse.builder()
                .sessionId(session.getSessionId())
                .appointmentId(session.getAppointment() != null ? session.getAppointment().getAppointmentId() : null)
                .appointment(session.getAppointment() != null ? AppointmentResponse.fromEntity(session.getAppointment()) : null)  // NEW
                .sessionToken(session.getSessionToken())
                .meetingUrl(session.getMeetingUrl())
                .roomId(session.getRoomId())
                .status(session.getStatus())
                .notaryJoinedAt(session.getNotaryJoinedAt())
                .clientJoinedAt(session.getClientJoinedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .notes(session.getNotes())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}



