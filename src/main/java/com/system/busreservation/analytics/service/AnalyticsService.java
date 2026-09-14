package com.system.busreservation.analytics.service;

import com.system.busreservation.analytics.dto.DashboardSummaryDto;
import com.system.busreservation.analytics.dto.OccupancyDto;
import com.system.busreservation.analytics.dto.PopularRouteDto;
import com.system.busreservation.analytics.dto.RevenueDto;
import com.system.busreservation.analytics.dto.RouteAnalyticsDto;
import com.system.busreservation.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    // ============================================================
    // DASHBOARD SUMMARY
    // ============================================================

    public DashboardSummaryDto getDashboardSummary() {

        Object[] row = analyticsRepository.getDashboardSummary();

        double averageOccupancy =
                analyticsRepository.getAverageOccupancy();

        BigDecimal totalRevenue =
                toBigDecimal(row[1]);

        BigDecimal todayRevenue =
                toBigDecimal(row[5]);

        return new DashboardSummaryDto(

                toLong(row[0]),

                totalRevenue,

                toLong(row[2]),

                toLong(row[3]),

                roundTwoDecimals(averageOccupancy),

                toLong(row[4]),

                todayRevenue
        );
    }

    // ============================================================
    // REVENUE
    // ============================================================

    public List<RevenueDto> getRevenue(
            LocalDate start,
            LocalDate end) {

        List<Object[]> rows =
                analyticsRepository.getRevenueByDateRange(
                        start,
                        end
                );

        List<RevenueDto> result = new ArrayList<>();

        for (Object[] r : rows) {

            LocalDate date;

            if (r[0] instanceof Date) {
                date = ((Date) r[0]).toLocalDate();
            } else if (r[0] instanceof java.sql.Timestamp) {
                date = ((java.sql.Timestamp) r[0])
                        .toLocalDateTime()
                        .toLocalDate();
            } else {
                date = LocalDate.parse(r[0].toString());
            }

            result.add(
                    new RevenueDto(
                            date,
                            toBigDecimal(r[1]),
                            toLong(r[2])
                    )
            );
        }

        return result;
    }

    // ============================================================
    // ROUTE ANALYTICS
    // ============================================================

    public List<RouteAnalyticsDto> getRouteAnalytics() {

        List<Object[]> rows =
                analyticsRepository.getRouteAnalytics();

        List<RouteAnalyticsDto> result =
                new ArrayList<>();

        for (Object[] r : rows) {

            long routeId = toLong(r[0]);

            double occupancy =
                    analyticsRepository.getRouteOccupancy(routeId);

            result.add(
                    new RouteAnalyticsDto(

                            routeId,

                            toStringValue(r[1]),

                            toStringValue(r[2]),

                            toStringValue(r[3]),

                            toLong(r[4]),

                            toLong(r[5]),

                            toBigDecimal(r[6]),

                            roundTwoDecimals(occupancy)
                    )
            );
        }

        return result;
    }

    // ============================================================
    // OCCUPANCY
    // ============================================================

    public List<OccupancyDto> getOccupancy(
            LocalDate start,
            LocalDate end) {

        List<Object[]> rows =
                analyticsRepository.getOccupancy(
                        start,
                        end
                );

        List<OccupancyDto> result =
                new ArrayList<>();

        for (Object[] r : rows) {

            int totalSeats =
                    (int) toLong(r[4]);

            int bookedSeats =
                    (int) toLong(r[5]);

            double percentage;

            if (totalSeats == 0) {
                percentage = 0.0;
            } else {

                percentage =
                        ((double) bookedSeats * 100.0)
                                / totalSeats;
            }

            percentage =
                    roundTwoDecimals(percentage);

            result.add(
                    new OccupancyDto(

                            toLong(r[0]),

                            toStringValue(r[1]),

                            toStringValue(r[2]),

                            toStringValue(r[3]),

                            totalSeats,

                            bookedSeats,

                            percentage
                    )
            );
        }

        return result;
    }

    // ============================================================
    // POPULAR ROUTES
    // ============================================================

    public List<PopularRouteDto> getPopularRoutes() {

        List<Object[]> rows =
                analyticsRepository.getPopularRoutes();

        List<PopularRouteDto> result =
                new ArrayList<>();

        for (Object[] r : rows) {

            result.add(
                    new PopularRouteDto(

                            toStringValue(r[0]),

                            toStringValue(r[1]),

                            toStringValue(r[2]),

                            toLong(r[3]),

                            toBigDecimal(r[4])
                    )
            );
        }

        return result;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private long toLong(Object value) {

        if (value == null) {
            return 0L;
        }

        return ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {

        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(
                    ((Number) value).doubleValue()
            );
        }

        return new BigDecimal(value.toString());
    }

    private String toStringValue(Object value) {

        if (value == null) {
            return "";
        }

        return value.toString();
    }

    private double roundTwoDecimals(double value) {

        return BigDecimal.valueOf(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}