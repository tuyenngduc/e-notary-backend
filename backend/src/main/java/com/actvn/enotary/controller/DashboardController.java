package com.actvn.enotary.controller;

import com.actvn.enotary.dto.response.DashboardSummaryResponse;
import com.actvn.enotary.dto.response.RequestsChartDataResponse;
import com.actvn.enotary.dto.response.RevenueDataResponse;
import com.actvn.enotary.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RevenueDataResponse>> getRevenueData() {
        return ResponseEntity.ok(dashboardService.getRevenueData());
    }

    @GetMapping("/requests-chart")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RequestsChartDataResponse>> getRequestsChartData() {
        return ResponseEntity.ok(dashboardService.getRequestsChartData());
    }
}
