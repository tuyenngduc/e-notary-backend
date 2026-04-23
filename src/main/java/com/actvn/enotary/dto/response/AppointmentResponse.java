package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.enums.AppointmentStatus;
import com.actvn.enotary.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.actvn.enotary.enums.ContractType;

@Data
@Builder
public class AppointmentResponse {
    private UUID appointmentId;
    private UUID requestId;
    private ServiceType serviceType;
    private ContractType contractType;
    private OffsetDateTime scheduledTime;

    /** null nếu OFFLINE hoặc chưa tạo phòng họp */
    private String meetingUrl;

    /** null nếu ONLINE */
    private String physicalAddress;

    private AppointmentStatus status;
    private OffsetDateTime createdAt;

    // UI helper fields
    private String clientName;

    public static AppointmentResponse fromEntity(Appointment a) {
        String cName = null;
        if (a.getRequest() != null && a.getRequest().getClient() != null) {
            cName = a.getRequest().getClient().getProfile() != null 
                    ? a.getRequest().getClient().getProfile().getFullName() 
                    : a.getRequest().getClient().getEmail();
        }

        return AppointmentResponse.builder()
                .appointmentId(a.getAppointmentId())
                .requestId(a.getRequest() != null ? a.getRequest().getRequestId() : null)
                .serviceType(a.getRequest() != null ? a.getRequest().getServiceType() : null)
                .contractType(a.getRequest() != null ? a.getRequest().getContractType() : null)
                .scheduledTime(a.getScheduledTime())
                .meetingUrl(a.getMeetingUrl())
                .physicalAddress(a.getPhysicalAddress())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .clientName(cName)
                .build();
    }
}

