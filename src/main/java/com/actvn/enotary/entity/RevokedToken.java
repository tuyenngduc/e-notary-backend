package com.actvn.enotary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_token_jti", columnList = "jti")
})
@Data
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private Instant revokedAt = Instant.now();

    @Column
    private Instant expiresAt;

}

