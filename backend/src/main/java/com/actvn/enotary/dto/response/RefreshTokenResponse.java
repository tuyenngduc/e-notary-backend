package com.actvn.enotary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class RefreshTokenResponse {
    private String token;
    private String refreshToken;
    private String email;

}
