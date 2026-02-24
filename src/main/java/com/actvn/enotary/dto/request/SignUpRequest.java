package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank @Pattern(regexp = "^(0|\\+84)(\\d{9})$") private String phoneNumber;
    @NotBlank @Size(min = 6) private String password;
}