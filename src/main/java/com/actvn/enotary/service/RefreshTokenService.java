package com.actvn.enotary.service;

import com.actvn.enotary.entity.RefreshToken;
import com.actvn.enotary.entity.RevokedToken;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.RefreshTokenRepository;
import com.actvn.enotary.repository.RevokedTokenRepository;
import com.actvn.enotary.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtUtil jwtUtil;

    public RefreshToken createRefreshToken(String email) {
        String token = jwtUtil.generateRefreshToken(email);
        String jti = jwtUtil.extractJti(token);

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setToken(token);
        rt.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        // extract expiration
        Instant exp = jwtUtil.getClaims(token).getExpiration().toInstant();
        rt.setExpiresAt(exp);
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    @Transactional
    public RefreshToken verifyAndRotate(String refreshTokenStr) {
        if (!jwtUtil.validateToken(refreshTokenStr)) {
            log.warn("verifyAndRotate: invalid refresh token provided");
            throw new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        String jti = jwtUtil.extractJti(refreshTokenStr);
        RefreshToken rt = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new AppException("Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            log.warn("verifyAndRotate: refresh token revoked or expired for jti={}", jti);
            throw new AppException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        // revoke old
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        // create and return new
        return createRefreshToken(rt.getEmail());
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenStr) {
        if (refreshTokenStr == null) return;

        String token = refreshTokenStr.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        String jti = null;
        boolean valid = jwtUtil.validateToken(token);
        if (valid) {
            jti = jwtUtil.extractJti(token);
        } else {
            log.warn("revokeRefreshToken: token validation failed - will attempt DB lookup by raw token");
        }

        boolean handled = false;
        if (jti != null) {
            Optional<RefreshToken> opt = refreshTokenRepository.findByJti(jti);
            if (opt.isPresent()) {
                RefreshToken rt = opt.get();
                if (!rt.isRevoked()) {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("revoked refresh token by jti={}", jti);
                } else {
                    log.info("refresh token jti={} already revoked", jti);
                }
                handled = true;
            }
        }

        if (!handled) {
            // fallback: try find by token string
            Optional<RefreshToken> byToken = refreshTokenRepository.findByToken(token);
            if (byToken.isPresent()) {
                RefreshToken rt = byToken.get();
                if (!rt.isRevoked()) {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("revoked refresh token by raw token, jti={}", rt.getJti());
                } else {
                    log.info("refresh token (by raw) jti={} already revoked", rt.getJti());
                }
                handled = true;
            }
        }

        if (!handled) {
            log.info("revokeRefreshToken: no matching refresh token found to revoke for provided value");
        }
    }

    @Transactional
    public void revokeAllByEmail(String email) {
        if (email == null) return;
        String emailLower = email.toLowerCase(Locale.ROOT);
        List<RefreshToken> list = refreshTokenRepository.findByEmail(emailLower);
        for (RefreshToken rt : list) {
            if (!rt.isRevoked()) {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
                log.info("revoked refresh token jti={} for email={}", rt.getJti(), emailLower);
            }
        }
    }

    @Transactional
    public void revokeAccessToken(String accessTokenStr) {
        if (accessTokenStr == null) return;

        String token = accessTokenStr.trim();
        if (token.startsWith("Bearer ")) token = token.substring(7).trim();

        if (!jwtUtil.validateToken(token)) {
            log.warn("revokeAccessToken: invalid access token passed for revocation");
            return;
        }
        String jti = jwtUtil.extractJti(token);
        // store jti in revoked tokens
        RevokedToken revoked = new RevokedToken();
        revoked.setJti(jti);
        revoked.setExpiresAt(jwtUtil.getClaims(token).getExpiration().toInstant());
        revokedTokenRepository.save(revoked);
        log.info("revoked access token jti={}", jti);
    }

    public boolean isAccessTokenRevoked(String jti) {
        Optional<RevokedToken> r = revokedTokenRepository.findByJti(jti);
        return r.isPresent();
    }
}
