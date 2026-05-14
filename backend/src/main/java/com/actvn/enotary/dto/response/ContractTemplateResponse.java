package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.ContractTemplate;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ContractTemplateResponse {
    private UUID id;
    private UUID serviceTypeId;
    private String serviceTypeCode;
    private String name;
    private String fileUrl;
    private String version;
    private Boolean isActive;
    private OffsetDateTime updatedAt;

    public static ContractTemplateResponse fromEntity(ContractTemplate entity) {
        return ContractTemplateResponse.builder()
                .id(entity.getId())
                .serviceTypeId(entity.getServiceType() != null ? entity.getServiceType().getId() : null)
                .serviceTypeCode(entity.getServiceType() != null ? entity.getServiceType().getServiceCode() : null)
                .name(entity.getName())
                .fileUrl(entity.getFileUrl())
                .version(entity.getVersion())
                .isActive(entity.getIsActive())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
