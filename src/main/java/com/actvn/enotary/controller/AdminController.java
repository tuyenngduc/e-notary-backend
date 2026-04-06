package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.ApiResponse;
import com.actvn.enotary.dto.response.ApiResponseUtil;
import com.actvn.enotary.dto.response.UserResponse;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.exception.ErrorCode;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;

    @PostMapping("/notaries")
    public ResponseEntity<ApiResponse<UserResponse>> createNotary(
            Authentication authentication,
            @Valid @RequestBody SignUpRequest request) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getRole() != null ? userDetails.getRole().name() : "";
        if (!"ADMIN".equals(role)) {
            throw new AppException(ErrorCode.INVALID_AUTHORIZATION);
        }

        var created = userService.createNotary(request);
        URI location = URI.create("/api/users/" + created.getUserId());
        return ResponseEntity.created(location).body(
                ApiResponseUtil.created(UserResponse.fromUser(created), "Tạo công chứng viên thành công")
        );
    }
}

