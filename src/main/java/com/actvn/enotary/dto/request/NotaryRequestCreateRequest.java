package com.actvn.enotary.dto.request;

import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.enums.ContractType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotaryRequestCreateRequest {
    @NotNull(message = "Loại dịch vụ không được để trống")
    private ServiceType serviceType;

    @NotNull(message = "Loại hợp đồng không được để trống")
    private ContractType contractType;

    @Size(max = 1000)
    private String description;
}

