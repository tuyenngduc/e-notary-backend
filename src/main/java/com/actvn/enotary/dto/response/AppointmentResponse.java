package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.enums.AppointmentStatus;
import com.actvn.enotary.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {
    private UUID appointmentId;
    private UUID requestId;
    private ServiceType serviceType;
    private OffsetDateTime scheduledTime;

    /** null nếu OFFLINE hoặc chưa tạo phòng họp */
    private String meetingUrl;

    /** null nếu ONLINE */
    private String physicalAddress;

    private AppointmentStatus status;
    private OffsetDateTime createdAt;

    public static AppointmentResponse fromEntity(Appointment a) {
        return AppointmentResponse.builder()
                .appointmentId(a.getAppointmentId())
                .requestId(a.getRequest() != null ? a.getRequest().getRequestId() : null)
                .serviceType(a.getRequest() != null ? a.getRequest().getServiceType() : null)
                .scheduledTime(a.getScheduledTime())
                .meetingUrl(a.getMeetingUrl())
                .physicalAddress(a.getPhysicalAddress())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

