package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.modules.admin.dto.AnalyticsFilterRequest;
import com.system.busreservation.modules.admin.dto.DailyRevenuePoint;
import com.system.busreservation.modules.admin.dto.DashboardSummaryResponse;
import com.system.busreservation.modules.admin.dto.RevenueSummaryResponse;
import com.system.busreservation.modules.admin.dto.RoutePerformanceResponse;
import com.system.busreservation.modules.admin.repository.AdminBookingRepository;
import com.system.busreservation.modules.admin.repository.AdminFleetRepository;
import com.system.busreservation.modules.admin.repository.AdminRoutePerformanceRepository;
import com.system.busreservation.modules.admin.repository.AdminTripRepository;
import com.system.busreservation.modules.admin.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AdminBookingRepository bookingRepository;
    private final AdminRoutePerformanceRepository routePerformanceRepository;
    private final AdminFleetRepository fleetRepository;
    private final AdminTripRepository tripRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {

        LocalDate today = LocalDate.now();

        LocalDateTime from =
                today.withDayOfMonth(1).atStartOfDay();

        LocalDateTime to =
                today.plusDays(1).atStartOfDay();

        BigDecimal revenue =
                bookingRepository.sumRevenue(
                        from,
                        to,
                        null,
                        null
                );

        long bookings =
                bookingRepository.countBookings(
                        from,
                        to,
                        null,
                        null
                );

        long activeBuses =
                fleetRepository.count();

        long routes =
                routePerformanceRepository.count();

        List<RoutePerformanceResponse> topRoutes =
                getRoutePerformanceForPeriod(from, to)
                        .stream()
                        .limit(5)
                        .toList();

        return new DashboardSummaryResponse(
                revenue == null
                        ? BigDecimal.ZERO
                        : revenue,
                bookings,
                activeBuses,
                routes,
                topRoutes
        );
    }

    @Override
    public RevenueSummaryResponse getRevenueSummary(
            AnalyticsFilterRequest filter) {

        LocalDate dateFrom =
                filter.getDateFrom();

        LocalDate dateTo =
                filter.getDateTo();

        if (dateFrom == null) {
            dateFrom =
                    LocalDate.now()
                            .withDayOfMonth(1);
        }

        if (dateTo == null) {
            dateTo =
                    LocalDate.now();
        }

        LocalDateTime from =
                dateFrom.atStartOfDay();

        LocalDateTime to =
                dateTo.plusDays(1).atStartOfDay();

        Long routeId =
                filter.getRouteId();

        Long busId =
                filter.getBusId();

        BigDecimal revenue =
                bookingRepository.sumRevenue(
                        from,
                        to,
                        routeId,
                        busId
                );

        long bookings =
                bookingRepository.countBookings(
                        from,
                        to,
                        routeId,
                        busId
                );

        long seatsSold =
                bookingRepository.countSeatsSold(
                        from,
                        to,
                        routeId,
                        busId
                );

        long seatsAvailable =
                tripRepository.calculateAvailableSeats(
                        from,
                        to,
                        routeId,
                        busId
                );

        BigDecimal occupancyRate;

        if (seatsAvailable <= 0) {

            occupancyRate =
                    BigDecimal.ZERO;

        } else {

            double occupancy =
                    (seatsSold * 100.0)
                            / seatsAvailable;

            occupancyRate =
                    BigDecimal.valueOf(
                            Math.round(
                                    occupancy * 100.0
                            ) / 100.0
                    );
        }

        List<Object[]> rows =
                bookingRepository.dailyRevenueData(
                        from,
                        to,
                        routeId,
                        busId
                );

        Map<LocalDate, DailyRevenuePoint>
                trendMap =
                new LinkedHashMap<>();

        for (Object[] row : rows) {

            LocalDateTime bookingTime =
                    (LocalDateTime) row[0];

            LocalDate date =
                    bookingTime.toLocalDate();

            BigDecimal amount =
                    row[1] == null
                            ? BigDecimal.ZERO
                            : (BigDecimal) row[1];

            DailyRevenuePoint point =
                    trendMap.get(date);

            if (point == null) {

                point =
                        new DailyRevenuePoint();

                point.setDate(date);
                point.setRevenue(amount);
                point.setBookingCount(1);

                trendMap.put(
                        date,
                        point
                );

            } else {

                point.setRevenue(
                        point.getRevenue()
                                .add(amount)
                );

                point.setBookingCount(
                        point.getBookingCount() + 1
                );
            }
        }

        List<DailyRevenuePoint> trend =
                new ArrayList<>(
                        trendMap.values()
                );

        return new RevenueSummaryResponse(
                revenue == null
                        ? BigDecimal.ZERO
                        : revenue,
                bookings,
                seatsSold,
                occupancyRate,
                trend
        );
    }

    @Override
    public List<RoutePerformanceResponse> getRoutePerformance(
            AnalyticsFilterRequest filter) {

        LocalDate dateFrom =
                filter.getDateFrom() != null
                        ? filter.getDateFrom()
                        : LocalDate.now()
                        .withDayOfMonth(1);

        LocalDate dateTo =
                filter.getDateTo() != null
                        ? filter.getDateTo()
                        : LocalDate.now();

        LocalDateTime from =
                dateFrom.atStartOfDay();

        LocalDateTime to =
                dateTo.plusDays(1).atStartOfDay();

        return getRoutePerformanceForPeriod(from, to);
    }

    private List<RoutePerformanceResponse> getRoutePerformanceForPeriod(
            LocalDateTime from,
            LocalDateTime to) {

        List<RoutePerformanceResponse> routes =
                routePerformanceRepository.routePerformance(
                        from,
                        to
                );

        for (RoutePerformanceResponse route : routes) {

            long capacity =
                    routePerformanceRepository.calculateRouteCapacity(
                            route.getRouteId(),
                            from,
                            to
                    );

            long seatsSold =
                    routePerformanceRepository.calculateRouteSeatsSold(
                            route.getRouteId(),
                            from,
                            to
                    );

            double occupancy = 0.0;

            if (capacity > 0) {

                occupancy =
                        (seatsSold * 100.0)
                                / capacity;
            }

            route.setAverageOccupancyPercent(
                    Math.round(occupancy * 100.0) / 100.0
            );
        }

        return routes;
    }
}