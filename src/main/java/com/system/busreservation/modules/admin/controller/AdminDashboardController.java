package com.system.busreservation.modules.admin.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.admin.dto.AnalyticsFilterRequest;
import com.system.busreservation.modules.admin.dto.DashboardSummaryResponse;
import com.system.busreservation.modules.admin.dto.RevenueSummaryResponse;
import com.system.busreservation.modules.admin.dto.RoutePerformanceResponse;
import com.system.busreservation.modules.admin.service.AnalyticsService;
import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import com.system.busreservation.modules.admin.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;
    private final NotificationDispatchService notificationDispatchService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {

        DashboardSummaryResponse summary =
                analyticsService.getDashboardSummary();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard summary fetched",
                        summary
                )
        );
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueSummaryResponse>> getRevenue(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(required = false)
            Long routeId,

            @RequestParam(required = false)
            Long busId) {

        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();

        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setRouteId(routeId);
        filter.setBusId(busId);

        RevenueSummaryResponse summary =
                analyticsService.getRevenueSummary(filter);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Revenue summary fetched",
                        summary
                )
        );
    }

    @GetMapping("/routes/performance")
    public ResponseEntity<ApiResponse<List<RoutePerformanceResponse>>>
    getRoutePerformance(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo) {

        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();

        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);

        List<RoutePerformanceResponse> data =
                analyticsService.getRoutePerformance(filter);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Route performance fetched",
                        data
                )
        );
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(required = false)
            Long routeId,

            @RequestParam(required = false)
            Long busId) {

        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();

        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setRouteId(routeId);
        filter.setBusId(busId);

        RevenueSummaryResponse summary =
                analyticsService.getRevenueSummary(filter);

        byte[] csv =
                reportExportService.exportRevenueCsv(summary);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=revenue-report.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(required = false)
            Long routeId,

            @RequestParam(required = false)
            Long busId) {

        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();

        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setRouteId(routeId);
        filter.setBusId(busId);

        RevenueSummaryResponse summary =
                analyticsService.getRevenueSummary(filter);

        byte[] pdf =
                reportExportService.exportRevenuePdf(summary);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=revenue-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/dispatch-summary")
    public ResponseEntity<ApiResponse<Void>> dispatchSummary(
            @RequestBody Map<String, String> payload) {

        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.success("Email address is required", null)
            );
        }

        if (subject == null || subject.isBlank()) {
            subject = "CatchBus LK - Daily Operational Summary";
        }

        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.success("Email body is required", null)
            );
        }

        notificationDispatchService.dispatchOperationalSummary(
                to,
                subject,
                body
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Summary sent successfully to " + to,
                        null
                )
        );
    }
}