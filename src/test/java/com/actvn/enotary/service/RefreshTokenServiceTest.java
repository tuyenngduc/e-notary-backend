package com.actvn.enotary.service;

import com.actvn.enotary.entity.RefreshToken;
import com.actvn.enotary.entity.RevokedToken;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.RefreshTokenRepository;
import com.actvn.enotary.repository.RevokedTokenRepository;
import com.actvn.enotary.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, revokedTokenRepository, jwtUtil);
    }

    @Test
    void createRefreshTokenSavesToRepo() {
        when(jwtUtil.generateRefreshToken("u@e.com")).thenReturn("token123");
        Claims claims = mock(Claims.class);
        when(jwtUtil.extractJti("token123")).thenReturn("jti-1");
        when(jwtUtil.getClaims("token123")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(java.util.Date.from(Instant.now().plusSeconds(3600)));

        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RefreshToken rt = refreshTokenService.createRefreshToken("u@e.com");

        assertEquals("jti-1", rt.getJti());
        assertEquals("token123", rt.getToken());
        assertEquals("u@e.com", rt.getEmail());
        assertFalse(rt.isRevoked());
    }

    @Test
    void verifyAndRotateInvalidTokenThrows() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> refreshTokenService.verifyAndRotate("bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

}

