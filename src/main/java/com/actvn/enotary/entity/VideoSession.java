package com.actvn.enotary.entity;

import com.actvn.enotary.enums.VideoSessionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "video_sessions")
@Data
public class VideoSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;


    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;


    @Column(name = "session_token", unique = true)
    private String sessionToken;


    @Column(name = "meeting_url")
    private String meetingUrl;


    @Column(name = "room_id", unique = true)
    private String roomId;


    @Enumerated(EnumType.STRING)
    private VideoSessionStatus status = VideoSessionStatus.PENDING;


    private OffsetDateTime notaryJoinedAt;


    private OffsetDateTime clientJoinedAt;


    private OffsetDateTime endedAt;


    private Long durationSeconds;


    @Column(columnDefinition = "TEXT")
    private String notes;


    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();


    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

