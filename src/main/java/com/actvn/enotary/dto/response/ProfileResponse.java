package com.actvn.enotary.dto.response;

import com.actvn.enotary.entity.UserProfile;
import com.actvn.enotary.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID userId;
    private String email;
    private String phoneNumber;

    // profile fields (may be null if not yet provided)
    private String identityNumber;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String placeOfOrigin;
    private String placeOfResidence;
    private LocalDate issueDate;
    private String issuePlace;

    private VerificationStatus verificationStatus;

    public static ProfileResponse fromUser(com.actvn.enotary.entity.User user) {
        UserProfile p = user.getProfile();
        return ProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .identityNumber(p != null ? p.getIdentityNumber() : null)
                .fullName(p != null ? p.getFullName() : null)
                .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                .gender(p != null ? p.getGender() : null)
                .nationality(p != null ? p.getNationality() : null)
                .placeOfOrigin(p != null ? p.getPlaceOfOrigin() : null)
                .placeOfResidence(p != null ? p.getPlaceOfResidence() : null)
                .issueDate(p != null ? p.getIssueDate() : null)
                .issuePlace(p != null ? p.getIssuePlace() : null)
                .verificationStatus(user.getVerificationStatus())
                .build();
    }
}

