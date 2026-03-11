package com.actvn.enotary.enums;

/**
 * Trạng thái của Video Session
 */
public enum VideoSessionStatus {
    /**
     * Phòng họp được tạo nhưng chưa có ai vào
     */
    PENDING,

    /**
     * Notary đã vào phòng, chờ client
     */
    NOTARY_JOINED,

    /**
     * Cả Notary và Client đã vào phòng, cuộc gọi đang diễn ra
     */
    IN_PROGRESS,

    /**
     * Cuộc gọi kết thúc bình thường
     */
    FINISHED,

    /**
     * Cuộc gọi bị hủy hoặc timeout
     */
    CANCELLED
}

