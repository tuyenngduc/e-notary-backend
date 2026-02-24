package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.ProfileUpdateRequest;
import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.entity.UserProfile;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerClient(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã tồn tại", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException("Số điện thoại đã tồn tại", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT);
        user.setVerificationStatus(VerificationStatus.PENDING);
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setIdentityNumber(request.getIdentityNumber());
        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationality(request.getNationality());
        profile.setPlaceOfOrigin(request.getPlaceOfOrigin());
        profile.setPlaceOfResidence(request.getPlaceOfResidence());
        profile.setIssueDate(request.getIssueDate());
        profile.setIssuePlace(request.getIssuePlace());

        user.setProfile(profile);
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        return userRepository.save(user);
    }
}