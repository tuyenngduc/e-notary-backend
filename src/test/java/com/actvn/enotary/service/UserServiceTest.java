package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private SignUpRequest makeRequest(String email, String phone, String password) {
        SignUpRequest r = new SignUpRequest();
        r.setEmail(email);
        r.setPhoneNumber(phone);
        r.setPassword(password);
        return r;
    }

    @Test
    void registerClientSuccess() {
        String emailInput = "User@Example.COM";
        String phoneInput = "+84912345678"; // will normalize to 0912345678
        String password = "secret";

        SignUpRequest req = makeRequest(emailInput, phoneInput, password);

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashed-pass");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });

        User res = userService.registerClient(req);

        assertNotNull(res.getUserId());
        assertEquals("user@example.com", res.getEmail());
        assertEquals("0912345678", res.getPhoneNumber());
        assertEquals(Role.CLIENT, res.getRole());
        assertEquals(VerificationStatus.PENDING, res.getVerificationStatus());

        User saved = captor.getValue();
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("0912345678", saved.getPhoneNumber());
        assertEquals("hashed-pass", saved.getPasswordHash());
    }

    @Test
    void registerClientDuplicateEmailThrowsConflict() {
        SignUpRequest req = makeRequest("a@b.com", "0912345678", "p");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.registerClient(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Email đã tồn tại", ex.getMessage());
    }

    @Test
    void registerClientDuplicatePhoneThrowsConflict() {
        SignUpRequest req = makeRequest("a@b.com", "+84912345678", "p");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.registerClient(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Số điện thoại đã tồn tại", ex.getMessage());
    }

    @Test
    void registerClientSaveRaceThrowsConflict() {
        SignUpRequest req = makeRequest("a@b.com", "+84912345678", "p");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("h");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        AppException ex = assertThrows(AppException.class, () -> userService.registerClient(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Email hoặc số điện thoại đã tồn tại", ex.getMessage());
    }

    @Test
    void registerClientInvalidPhoneThrowsBadRequest() {
        SignUpRequest req = makeRequest("a@b.com", "12345", "p");
        // normalization will leave "12345" which is invalid

        AppException ex = assertThrows(AppException.class, () -> userService.registerClient(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Số điện thoại không hợp lệ", ex.getMessage());
    }

}

