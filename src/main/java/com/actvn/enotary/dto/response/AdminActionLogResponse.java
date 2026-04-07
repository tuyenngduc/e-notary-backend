package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminActionLogResponse {
    private UUID logId;
    private UUID actorUserId;
    private String actorEmail;
    private String action;
    private UUID targetUserId;
    private String oldValue;
    private String newValue;
    private OffsetDateTime timestamp;

    public static AdminActionLogResponse fromEntity(AuditLog log) {
        return AdminActionLogResponse.builder()
                .logId(log.getLogId())
                .actorUserId(log.getUser() != null ? log.getUser().getUserId() : null)
                .actorEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .action(log.getAction())
                .targetUserId(log.getRecordId())
                .oldValue(log.getOldValue() != null ? log.getOldValue().toString() : null)
                .newValue(log.getNewValue() != null ? log.getNewValue().toString() : null)
                .timestamp(log.getTimestamp())
                .build();
    }
}

