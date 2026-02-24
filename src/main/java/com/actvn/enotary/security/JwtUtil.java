package com.actvn.enotary.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Tạo token từ Authentication (access token) — include jti
     */
    public String generateToken(Authentication authentication) {

        String email = authentication.getName();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Tạo token với thêm claims (role, userId...) — access token with jti
     */
    public String generateToken(String email, Map<String, Object> claims) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate refresh token (longer expiration) with jti
     */
    public String generateRefreshToken(String email) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate token with custom expiration (ms) — utility
     */
    public String generateTokenWithCustomExpiration(String email, Map<String, Object> claims, long ttlMillis) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Lấy email từ token
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

     /**
     * Lấy role từ token
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Extract jti
     */
    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Public accessor to claims (so services can read expiry/jti)
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Return claims even if token expired (returns claims from ExpiredJwtException)
     */
    public Claims getClaimsAllowExpired(String token) {
        try {
            return getClaims(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    /**
     * Parse claims chung (deprecated private)
     */
    private Claims parseClaims(String token) {
        return getClaims(token);
    }
}