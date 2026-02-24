package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.LoginRequest;
import com.actvn.enotary.dto.request.RefreshRequest;
import com.actvn.enotary.dto.response.LoginResponse;
import com.actvn.enotary.dto.response.RefreshTokenResponse;
import com.actvn.enotary.service.AuthService;
import com.actvn.enotary.service.RefreshTokenService;
import com.actvn.enotary.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        var newRefresh = refreshTokenService.verifyAndRotate(request.getRefreshToken());

        String email = newRefresh.getEmail();
        var access = authService.createAccessTokenForEmail(email);

        return ResponseEntity.ok(new RefreshTokenResponse(
                access,
                newRefresh.getToken(),
                email

        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody(required = false) RefreshRequest request) {


        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7);
            // revoke access token jti
            refreshTokenService.revokeAccessToken(accessToken);

            // if no refresh token provided, also revoke all refresh tokens for this user
            if (request == null || request.getRefreshToken() == null) {
                var claims = jwtUtil.getClaimsAllowExpired(accessToken);
                String email = claims.getSubject();
                refreshTokenService.revokeAllByEmail(email);
            }
        }


        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
