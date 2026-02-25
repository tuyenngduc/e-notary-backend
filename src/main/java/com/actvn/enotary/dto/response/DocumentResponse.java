package com.actvn.enotary.dto.response;

import com.actvn.enotary.enums.DocType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID documentId;
    private UUID requestId;
    private String filePath;
    private String absolutePath;
    private DocType docType;
    private String fileHash;
    private OffsetDateTime createdAt;

    public static DocumentResponse fromEntity(com.actvn.enotary.entity.Document d) {
        return DocumentResponse.builder()
                .documentId(d.getDocumentId())
                .requestId(d.getRequest() != null ? d.getRequest().getRequestId() : null)
                .filePath(d.getFilePath())
                .absolutePath(null)
                .docType(d.getDocType())
                .fileHash(d.getFileHash())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
