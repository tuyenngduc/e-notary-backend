package com.actvn.enotary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalUsers;
    private long totalNotaries;
    private long pendingRequests;
    private long completedRequests;
}
