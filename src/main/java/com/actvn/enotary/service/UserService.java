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
import org.springframework.dao.DataIntegrityViolationException;
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
        // Normalize email and phone to avoid duplicates caused by formatting/case
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhoneNumber());

        // Validate normalized phone (must be 0 + 9 digits)
        if (phone == null || !phone.matches("^0\\d{9}$")) {
            throw new AppException("Số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(email)) {
            throw new AppException("Email đã tồn tại", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new AppException("Số điện thoại đã tồn tại", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT);
        user.setVerificationStatus(VerificationStatus.PENDING);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another request may have inserted same email/phone concurrently
            throw new AppException("Email hoặc số điện thoại đã tồn tại", HttpStatus.CONFLICT);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        // remove spaces, dashes, parentheses
        String cleaned = phone.replaceAll("[\\s\\-()]+", "");
        // keep leading + if present
        cleaned = cleaned.replaceAll("[^+0-9]", "");
        if (cleaned.startsWith("+84")) {
            // +84xxxxxxxxx -> 0xxxxxxxxx
            String rest = cleaned.substring(3);
            return "0" + rest;
        }
        return cleaned;
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