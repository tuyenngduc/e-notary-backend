package com.actvn.enotary.entity;

import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.ServiceType;
import jakarta.persistence.*;
import lombok.Data;

import javax.swing.text.Document;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notary_requests")
@Data
public class NotaryRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID requestId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne
    @JoinColumn(name = "notary_id")
    private User notary;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String contractType;
    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.NEW;

    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL)
    private List<Document> documents;
}