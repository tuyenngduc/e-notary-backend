package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.ProfileUpdateRequest;
import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.UserResponse;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.entity.UserProfile;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.UserProfileRepository;
import com.actvn.enotary.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Value("${app.admin.email:}")
    private String defaultAdminEmail;

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

    @Transactional
    public User createNotary(SignUpRequest request, UUID adminUserId) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhoneNumber());

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException("Không tìm thấy quản trị viên", HttpStatus.UNAUTHORIZED));

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
        user.setRole(Role.NOTARY);
        user.setVerificationStatus(VerificationStatus.PENDING);
        try {
            User savedUser = userRepository.save(user);
            auditLogService.logAction(admin, "NOTARY_CREATED", "users", savedUser.getUserId());
            return savedUser;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
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

        // check duplicate identity number (unique across profiles)
        if (request.getIdentityNumber() != null && !request.getIdentityNumber().isBlank()) {
            Optional<UserProfile> existing = userProfileRepository.findByIdentityNumber(request.getIdentityNumber());
            if (existing.isPresent() && !existing.get().getUser().getUserId().equals(userId)) {
                throw new AppException("Số CCCD đã được sử dụng bởi người dùng khác", HttpStatus.CONFLICT);
            }
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }

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
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException("Dữ liệu không hợp lệ hoặc trùng lặp", HttpStatus.CONFLICT);
        }
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    public Page<UserResponse> getUsers(Role role, VerificationStatus verificationStatus, Pageable pageable) {
        Specification<User> specification = Specification.where(null);

        if (role != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("role"), role));
        }

        if (verificationStatus != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("verificationStatus"), verificationStatus));
        }

        return userRepository.findAll(specification, pageable).map(UserResponse::fromUser);
    }

    @Transactional
    public User deleteUserByAdmin(UUID targetUserId, UUID adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException("Không tìm thấy quản trị viên", HttpStatus.UNAUTHORIZED));

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (target.getUserId().equals(adminUserId)) {
            throw new AppException("Không thể xóa chính tài khoản quản trị đang đăng nhập", HttpStatus.BAD_REQUEST);
        }

        String normalizedDefaultAdmin = normalizeEmail(defaultAdminEmail);
        if (normalizedDefaultAdmin != null
                && !normalizedDefaultAdmin.isBlank()
                && normalizedDefaultAdmin.equals(normalizeEmail(target.getEmail()))) {
            throw new AppException("Không thể xóa tài khoản admin mặc định", HttpStatus.BAD_REQUEST);
        }

        refreshTokenService.revokeAllByEmail(target.getEmail());

        try {
            userRepository.delete(target);
            userRepository.flush();
            auditLogService.logAction(admin, "USER_DELETED", "users", target.getUserId());
            return target;
        } catch (DataIntegrityViolationException ex) {
            throw new AppException("Không thể xóa tài khoản vì đang có dữ liệu liên quan", HttpStatus.CONFLICT);
        }
    }
}
