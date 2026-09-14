package com.system.busreservation.analytics.controller;

import com.system.busreservation.analytics.dto.*;
import com.system.busreservation.analytics.service.AnalyticsService;
import com.system.busreservation.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/static/analytics")
@RequiredArgsConstructor
@CrossOrigin
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboard() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        analyticsService.getDashboardSummary()
                )
        );
    }

    // Revenue
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueDto>>> getRevenue(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        analyticsService.getRevenue(startDate, endDate)
                )
        );
    }

    // Route Analytics
    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<RouteAnalyticsDto>>> getRouteAnalytics() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        analyticsService.getRouteAnalytics()
                )
        );
    }

    // Occupancy
    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<List<OccupancyDto>>> getOccupancy(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        analyticsService.getOccupancy(startDate, endDate)
                )
        );
    }

    // Popular Routes
    @GetMapping("/popular-routes")
    public ResponseEntity<ApiResponse<List<PopularRouteDto>>> getPopularRoutes() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        analyticsService.getPopularRoutes()
                )
        );
    }
}