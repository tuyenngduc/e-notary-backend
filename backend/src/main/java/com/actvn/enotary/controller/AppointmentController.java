package com.actvn.enotary.controller;

import com.actvn.enotary.dto.response.AppointmentResponse;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.NotaryRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final NotaryRequestService notaryRequestService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('NOTARY')")
    public ResponseEntity<com.actvn.enotary.dto.response.ApiResponse<List<AppointmentResponse>>> getMyAppointments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AppointmentResponse> appointments = notaryRequestService.getMyAppointments(userDetails.getId());
        return ResponseEntity.ok(com.actvn.enotary.dto.response.ApiResponseUtil.success(appointments));
    }
}
