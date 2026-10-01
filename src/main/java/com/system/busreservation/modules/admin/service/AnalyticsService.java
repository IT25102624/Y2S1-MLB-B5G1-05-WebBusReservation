package com.system.busreservation.modules.admin.service;

import com.system.busreservation.modules.admin.dto.AnalyticsFilterRequest;
import com.system.busreservation.modules.admin.dto.DashboardSummaryResponse;
import com.system.busreservation.modules.admin.dto.RevenueSummaryResponse;
import com.system.busreservation.modules.admin.dto.RoutePerformanceResponse;

import java.util.List;

public interface AnalyticsService {

    DashboardSummaryResponse getDashboardSummary();

    RevenueSummaryResponse getRevenueSummary(
            AnalyticsFilterRequest filter
    );

    List<RoutePerformanceResponse> getRoutePerformance(
            AnalyticsFilterRequest filter
    );
}