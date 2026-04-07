package com.actvn.enotary.service;

import com.actvn.enotary.entity.AuditLog;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void logAction(User actor, String action, String tableName, UUID recordId,
                         String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setAction(action);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setOldValue(toJsonNode(oldValue));
        log.setNewValue(toJsonNode(newValue));
        log.setTimestamp(OffsetDateTime.now());
        auditLogRepository.save(log);
    }

    private JsonNode toJsonNode(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new AppException("Dữ liệu lịch sử thao tác không phải JSON hợp lệ", HttpStatus.BAD_REQUEST);
        }
    }

    public void logAction(User actor, String action, String tableName, UUID recordId) {
        logAction(actor, action, tableName, recordId, null, null);
    }

    public List<AuditLog> getActionHistory(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findByActionInOrderByTimestampDesc(
            List.of("NOTARY_CREATED", "NOTARY_ACCESS_REVOKED", "NOTARY_ACCESS_RESTORED"),
            pageable
        );
    }
}

