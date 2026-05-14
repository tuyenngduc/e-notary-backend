package com.actvn.enotary.service;

import com.actvn.enotary.dto.response.DashboardSummaryResponse;
import com.actvn.enotary.dto.response.RequestsChartDataResponse;
import com.actvn.enotary.dto.response.RevenueDataResponse;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.PaymentRepository;
import com.actvn.enotary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserRepository userRepository;
    private final NotaryRequestRepository notaryRequestRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalUsers = userRepository.countByRole(Role.CLIENT);
        long totalNotaries = userRepository.countByRole(Role.NOTARY);

        List<RequestStatus> pendingStatuses = List.of(
                RequestStatus.NEW,
                RequestStatus.PROCESSING,
                RequestStatus.ACCEPTED,
                RequestStatus.SCHEDULED,
                RequestStatus.IN_VIDEO_CALL,
                RequestStatus.AWAITING_PAYMENT
        );
        long pendingRequests = notaryRequestRepository.countByStatusIn(pendingStatuses);

        List<RequestStatus> completedStatuses = List.of(RequestStatus.COMPLETED);
        long completedRequests = notaryRequestRepository.countByStatusIn(completedStatuses);

        return DashboardSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalNotaries(totalNotaries)
                .pendingRequests(pendingRequests)
                .completedRequests(completedRequests)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RevenueDataResponse> getRevenueData() {
        List<Object[]> results = paymentRepository.getMonthlyRevenue();
        List<RevenueDataResponse> revenueData = new ArrayList<>();
        for (Object[] row : results) {
            String month = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            revenueData.add(new RevenueDataResponse(month, revenue));
        }
        return revenueData;
    }

    @Transactional(readOnly = true)
    public List<RequestsChartDataResponse> getRequestsChartData() {
        List<Object[]> results = notaryRequestRepository.countRequestsByServiceType();
        List<RequestsChartDataResponse> chartData = new ArrayList<>();
        for (Object[] row : results) {
            ServiceType serviceType = (ServiceType) row[0];
            Long count = (Long) row[1];
            chartData.add(new RequestsChartDataResponse(serviceType.name(), count));
        }
        return chartData;
    }
}
