package com.actvn.enotary.dto.response;

import com.actvn.enotary.enums.DocType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentRequirementResponse {
    private List<DocType> requiredDocTypes;
    private List<DocType> uploadedDocTypes;
    private List<DocType> missingDocTypes;
    private boolean readyForAccept;
}

