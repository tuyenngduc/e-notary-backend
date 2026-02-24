package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.SignUpRequest;
import com.actvn.enotary.dto.response.UserResponse;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> registerClient(
            @Valid @RequestBody SignUpRequest request) {

        User newUser = userService.registerClient(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.fromUser(newUser));
    }
}