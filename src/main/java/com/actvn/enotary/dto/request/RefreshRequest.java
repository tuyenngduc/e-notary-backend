package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}

