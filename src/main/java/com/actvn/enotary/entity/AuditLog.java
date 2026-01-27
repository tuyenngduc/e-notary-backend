package com.actvn.enotary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID logId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String action;
    private String tableName;
    private UUID recordId;

    @Column(columnDefinition = "jsonb")
    private String oldValue;

    @Column(columnDefinition = "jsonb")
    private String newValue;

    private OffsetDateTime timestamp = OffsetDateTime.now();
}
