package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.LoginRequest;
import com.actvn.enotary.dto.response.LoginResponse;
import com.actvn.enotary.entity.RefreshToken;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.security.CustomUserDetailsService;
import com.actvn.enotary.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtUtil, refreshTokenService, customUserDetailsService);
    }

    private LoginRequest makeRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private User makeUser(String email) {
        User u = new User();
        u.setUserId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole(Role.CLIENT);
        return u;
    }

    @Test
    void loginSuccessReturnsTokenAndUserInfo() {
        String email = "user@example.com";
        User user = makeUser(email);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(eq(user.getEmail()), anyMap())).thenReturn("jwt-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");
        when(refreshTokenService.createRefreshToken(eq(user.getEmail()))).thenReturn(rt);

        LoginResponse resp = authService.login(makeRequest(email, "password"));

        assertNotNull(resp);
        assertEquals("jwt-token", resp.getToken());
        assertEquals("refresh-token", resp.getRefreshToken());
        assertEquals(email, resp.getEmail());
        assertEquals(user.getRole().name(), resp.getRole());
    }

    @Test
    void loginBadCredentialsThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    void loginUsernameNotFoundThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("no"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    void loginDisabledThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Account is disabled. Contact support.", ex.getMessage());
    }

    @Test
    void loginLockedThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        assertEquals("Account is locked. Try again later or contact support.", ex.getMessage());
    }

    @Test
    void loginCredentialsExpiredThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new CredentialsExpiredException("expired"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Credentials expired. Reset your password.", ex.getMessage());
    }

    @Test
    void loginGenericAuthenticationExceptionThrowsAppException() {
        when(authenticationManager.authenticate(any())).thenThrow(new AuthenticationServiceException("service"));

        AppException ex = assertThrows(AppException.class, () -> authService.login(makeRequest("u", "p")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Authentication failed.", ex.getMessage());
    }

}
