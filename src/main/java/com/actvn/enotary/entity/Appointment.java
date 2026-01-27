package com.actvn.enotary.entity;

import com.actvn.enotary.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID appointmentId;

    @OneToOne
    @JoinColumn(name = "request_id", nullable = false)
    private NotaryRequest request;

    private LocalDateTime scheduledTime;
    private String meetingUrl; //Custom WebRTC
    private String physicalAddress = "Văn phòng công chứng số 1";

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private OffsetDateTime createdAt = OffsetDateTime.now();
}
