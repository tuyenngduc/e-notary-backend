package com.actvn.enotary.startup;

import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import com.actvn.enotary.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class StartupAdminInitializer {
    private static final Logger log = LoggerFactory.getLogger(StartupAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.phone:}")
    private String adminPhone;

    private final Random rand = new Random();

    @PostConstruct
    public void ensureDefaultAdmin() {
        long count = userRepository.countByRole(Role.ADMIN);
        if (count > 0) {
            log.info("Admin user(s) present: {}", count);
            return;
        }

        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No admin email configured (app.admin.email). Skipping default admin creation.");
            return;
        }

        try {
            String phoneToUse = null;
            if (adminPhone != null && !adminPhone.isBlank()) {
                phoneToUse = adminPhone.trim();
                if (userRepository.existsByPhoneNumber(phoneToUse)) {
                    log.warn("Configured admin phone {} is already used by another user", phoneToUse);
                    phoneToUse = null;
                }
            }

            if (phoneToUse == null) {
                for (int i = 0; i < 50; i++) {
                    String gen = String.format("096%07d", Math.abs(rand.nextInt(10_000_000)));
                    if (!userRepository.existsByPhoneNumber(gen)) {
                        phoneToUse = gen;
                        break;
                    }
                }
            }

            if (phoneToUse == null) {
                log.error("Unable to find a unique phone number for admin creation. Skipping admin creation.");
                return;
            }

            Optional<User> existing = userRepository.findByEmail(adminEmail);
            if (existing.isPresent()) {
                User u = existing.get();
                boolean changed = false;
                if (u.getRole() != Role.ADMIN) {
                    u.setRole(Role.ADMIN);
                    changed = true;
                }
                if (u.getVerificationStatus() != VerificationStatus.VERIFIED) {
                    u.setVerificationStatus(VerificationStatus.VERIFIED);
                    changed = true;
                }
                if (adminPassword != null && !adminPassword.isBlank()) {
                    u.setPasswordHash(passwordEncoder.encode(adminPassword));
                    changed = true;
                }
                if (u.getPhoneNumber() == null || u.getPhoneNumber().isBlank()) {
                    u.setPhoneNumber(phoneToUse);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(u);
                    log.info("Promoted existing user to ADMIN: {}", adminEmail);
                } else {
                    log.info("Existing user already ADMIN: {}", adminEmail);
                }
            } else {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPhoneNumber(phoneToUse);
                if (adminPassword != null && !adminPassword.isBlank()) {
                    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                } else {
                    admin.setPasswordHash("");
                }
                admin.setRole(Role.ADMIN);
                admin.setVerificationStatus(VerificationStatus.VERIFIED);
                userRepository.save(admin);
                log.info("Created new ADMIN user: {}", adminEmail);
            }
        } catch (Exception ex) {
            log.error("Failed to ensure default admin user", ex);
        }
    }
}
