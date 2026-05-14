package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.RefreshRequest;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.service.RefreshTokenService;
import com.actvn.enotary.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(null, refreshTokenService, jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.actvn.enotary.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void refreshEndpointRejectsInvalidRefresh() throws Exception {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("bad");

        when(refreshTokenService.verifyAndRotate("bad")).thenThrow(new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

}
