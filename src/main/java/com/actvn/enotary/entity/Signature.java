package com.actvn.enotary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "signatures")
@Data
public class Signature {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID signatureId;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String signatureValue; // Giá trị mã hóa của chữ ký

    private String certSerial; // Serial của chứng thư số (CA)
    private OffsetDateTime signedAt = OffsetDateTime.now();
    private Boolean isValid = true;
}