package com.actvn.enotary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String email;
    private String role;
}
