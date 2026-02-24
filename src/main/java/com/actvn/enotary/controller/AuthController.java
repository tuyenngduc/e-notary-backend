package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.ProfileUpdateRequest;
import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.UserResponse;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register/client")
    public ResponseEntity<UserResponse> registerClient(@Valid @RequestBody SignUpRequest request) {
        User newUser = userService.registerClient(request);


        UserResponse response = UserResponse.fromUser(newUser);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
