package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NotaryRequestResponse {
    private UUID requestId;
    private UUID clientId;
    private UUID notaryId; // may be null
    private String serviceType;
    private String contractType;
    private String description;
    private RequestStatus status;
    private String rejectionReason;
    private String meetingUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<String> documentIds; // just ids for now
    private DocumentRequirementResponse documentRequirements;

    public static NotaryRequestResponse fromEntity(NotaryRequest r) {
        return fromEntity(r, null, null);
    }

    public static NotaryRequestResponse fromEntity(NotaryRequest r, DocumentRequirementResponse documentRequirements) {
        return fromEntity(r, documentRequirements, null);
    }

    public static NotaryRequestResponse fromEntity(NotaryRequest r, DocumentRequirementResponse documentRequirements, String meetingUrl) {
        return NotaryRequestResponse.builder()
                .requestId(r.getRequestId())
                .clientId(r.getClient() != null ? r.getClient().getUserId() : null)
                .notaryId(r.getNotary() != null ? r.getNotary().getUserId() : null)
                .serviceType(r.getServiceType() != null ? r.getServiceType().name() : null)
                .contractType(r.getContractType() != null ? r.getContractType().name() : null)
                .description(r.getDescription())
                .status(r.getStatus())
                .rejectionReason(r.getRejectionReason())
                .meetingUrl(meetingUrl)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .documentIds(r.getDocuments() != null ? r.getDocuments().stream().map(d -> d.getDocumentId().toString()).toList() : List.of())
                .documentRequirements(documentRequirements)
                .build();
    }
}

