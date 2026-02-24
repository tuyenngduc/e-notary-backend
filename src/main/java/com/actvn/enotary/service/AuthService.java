package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.LoginRequest;
import com.actvn.enotary.dto.response.LoginResponse;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.security.CustomUserDetailsService;
import com.actvn.enotary.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    public LoginResponse login(LoginRequest request) {

        Authentication authentication;
        try {
            authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail(),
                                    request.getPassword()
                            )
                    );
        } catch (BadCredentialsException | UsernameNotFoundException ex) {

            throw new AppException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        } catch (DisabledException ex) {
            throw new AppException("Account is disabled. Contact support.", HttpStatus.FORBIDDEN);
        } catch (LockedException ex) {
            throw new AppException("Account is locked. Try again later or contact support.", HttpStatus.LOCKED);
        } catch (CredentialsExpiredException ex) {
            throw new AppException("Credentials expired. Reset your password.", HttpStatus.UNAUTHORIZED);
        } catch (AuthenticationException ex) {

            throw new AppException("Authentication failed.", HttpStatus.UNAUTHORIZED);
        }

        var userDetails = (CustomUserDetails) authentication.getPrincipal();


        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getRole().name());
        claims.put("userId", userDetails.getId().toString());

        String token = jwtUtil.generateToken(
                userDetails.getUsername(),
                claims
        );

        var refresh = refreshTokenService.createRefreshToken(userDetails.getUsername());

        return new LoginResponse(
                token,
                refresh.getToken(),
                userDetails.getUsername(),
                userDetails.getRole().name()
        );
    }

    public String createAccessTokenForEmail(String email) {
        var userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getRole().name());
        claims.put("userId", userDetails.getId().toString());
        return jwtUtil.generateToken(email, claims);
    }
}