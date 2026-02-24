package com.actvn.enotary.security;

import com.actvn.enotary.service.RefreshTokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public JwtFilter(JwtUtil jwtUtil,
                     RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {

                String jti = jwtUtil.extractJti(token);
                // if token is revoked, don't authenticate
                if (refreshTokenService.isAccessTokenRevoked(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                var authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}