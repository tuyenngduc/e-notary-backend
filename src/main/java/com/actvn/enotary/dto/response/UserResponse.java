package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID userId;
    private String email;
    private String phoneNumber;
    private Role role;
    private VerificationStatus verificationStatus;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .verificationStatus(user.getVerificationStatus())
                .build();
    }
}
