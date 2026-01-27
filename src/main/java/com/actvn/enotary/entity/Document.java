package com.actvn.enotary.entity;

import com.actvn.enotary.enums.DocType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID documentId;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private NotaryRequest request;

    private String filePath;

    @Enumerated(EnumType.STRING)
    private DocType docType;

    private String fileHash;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}