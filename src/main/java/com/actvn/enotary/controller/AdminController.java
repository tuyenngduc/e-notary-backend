package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.AdminActionLogResponse;
import com.actvn.enotary.dto.response.ApiResponse;
import com.actvn.enotary.dto.response.ApiResponseUtil;
import com.actvn.enotary.dto.response.UserResponse;
import com.actvn.enotary.entity.AuditLog;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.exception.ErrorCode;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.AuditLogService;
import com.actvn.enotary.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            Authentication authentication,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @PageableDefault(size = 10, sort = "email") Pageable pageable) {
        ensureAdmin(authentication);
        return ResponseEntity.ok(
                ApiResponseUtil.success(
                        userService.getUsers(role, verificationStatus, pageable),
                        "Lấy danh sách người dùng thành công"
                )
        );
    }

    @PostMapping("/notaries")
    public ResponseEntity<ApiResponse<UserResponse>> createNotary(
            Authentication authentication,
            @Valid @RequestBody SignUpRequest request) {

        CustomUserDetails admin = ensureAdmin(authentication);

        var created = userService.createNotary(request, admin.getId());
        URI location = URI.create("/api/users/" + created.getUserId());
        return ResponseEntity.created(location).body(
                ApiResponseUtil.created(UserResponse.fromUser(created), "Tạo công chứng viên thành công")
        );
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> deleteUser(
            Authentication authentication,
            @PathVariable UUID userId) {
        CustomUserDetails admin = ensureAdmin(authentication);
        UserResponse deleted = UserResponse.fromUser(userService.deleteUserByAdmin(userId, admin.getId()));
        return ResponseEntity.ok(ApiResponseUtil.success(deleted, "Xóa tài khoản thành công"));
    }

    @GetMapping("/notary-access-history")
    public ResponseEntity<ApiResponse<List<AdminActionLogResponse>>> getActionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit) {
        ensureAdmin(authentication);
        List<AuditLog> logs = auditLogService.getActionHistory(limit);
        List<AdminActionLogResponse> responses = logs.stream()
                .map(AdminActionLogResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(
                ApiResponseUtil.success(responses, "Lấy lịch sử thao tác thành công")
        );
    }

    private CustomUserDetails ensureAdmin(Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION);
        }

        String role = userDetails.getRole() != null ? userDetails.getRole().name() : "";
        if (!"ADMIN".equals(role)) {
            throw new AppException(ErrorCode.INVALID_AUTHORIZATION);
        }

        return userDetails;
    }
}

