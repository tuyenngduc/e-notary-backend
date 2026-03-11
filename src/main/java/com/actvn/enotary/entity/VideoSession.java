package com.actvn.enotary.entity;

import com.actvn.enotary.enums.VideoSessionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity lưu trữ thông tin Video Call Session cho các cuộc họp Notary Online
 */
@Entity
@Table(name = "video_sessions")
@Data
public class VideoSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    /**
     * Link reference đến appointment
     */
    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    /**
     * Token WebRTC session - dùng để tạo connection P2P
     * Format có thể là: UUID-based token hoặc JWT
     */
    @Column(name = "session_token", unique = true)
    private String sessionToken;

    /**
     * URL công khai để access video room
     * Ví dụ: https://meet.domain.com/room/{roomId}
     */
    @Column(name = "meeting_url")
    private String meetingUrl;

    /**
     * Room ID cho video session (dùng nội bộ)
     */
    @Column(name = "room_id", unique = true)
    private String roomId;

    /**
     * Trạng thái của video session
     */
    @Enumerated(EnumType.STRING)
    private VideoSessionStatus status = VideoSessionStatus.PENDING;

    /**
     * Timestamp khi notary vào room
     */
    private OffsetDateTime notaryJoinedAt;

    /**
     * Timestamp khi client vào room
     */
    private OffsetDateTime clientJoinedAt;

    /**
     * Timestamp khi call kết thúc
     */
    private OffsetDateTime endedAt;

    /**
     * Thời gian kết nối (tính bằng giây)
     */
    private Long durationSeconds;

    /**
     * Ghi chú hoặc lý do kết thúc (nếu bị hủy)
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Thời gian tạo record
     */
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Thời gian cập nhật lần cuối
     */
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

