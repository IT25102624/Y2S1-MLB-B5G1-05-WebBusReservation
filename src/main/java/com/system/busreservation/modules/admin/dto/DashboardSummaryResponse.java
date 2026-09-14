package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalRevenueThisMonth;
    private long totalBookingsThisMonth;
    private long totalActiveBuses;
    private long totalRoutes;
    private List<RoutePerformanceResponse> topRoutes;
}