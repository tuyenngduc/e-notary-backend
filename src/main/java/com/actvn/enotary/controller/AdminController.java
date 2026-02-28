package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.UserResponse;
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
    public ResponseEntity<UserResponse> createNotary(
            Authentication authentication,
            @Valid @RequestBody SignUpRequest request) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getRole() != null ? userDetails.getRole().name() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        var created = userService.createNotary(request);
        URI location = URI.create("/api/users/" + created.getUserId());
        return ResponseEntity.created(location).body(com.actvn.enotary.dto.response.UserResponse.fromUser(created));
    }
}

