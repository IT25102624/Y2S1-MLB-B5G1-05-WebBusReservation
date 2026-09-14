package com.system.busreservation.modules.admin.repository;

import com.system.busreservation.model.Route;
import com.system.busreservation.modules.admin.dto.RoutePerformanceResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminRoutePerformanceRepository
        extends JpaRepository<Route, Long> {

    @Query("""
           SELECT new com.system.busreservation.modules.admin.dto.RoutePerformanceResponse(
               r.routeId,
               r.sourceCity,
               r.destinationCity,
               COUNT(DISTINCT t.tripId),
               COUNT(DISTINCT b.bookingId),
               COALESCE(SUM(b.totalAmount), 0),
               0.0
           )
           FROM Route r
           LEFT JOIN Trip t ON t.route = r
               AND t.departureTime >= :from
               AND t.departureTime < :to
           LEFT JOIN Booking b ON b.trip = t
               AND b.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
               AND b.bookingTime >= :from
               AND b.bookingTime < :to
           GROUP BY
               r.routeId,
               r.sourceCity,
               r.destinationCity
           ORDER BY COALESCE(SUM(b.totalAmount), 0) DESC
           """)
    List<RoutePerformanceResponse> routePerformance(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
           SELECT COALESCE(SUM(t.bus.totalSeats), 0)
           FROM Trip t
           WHERE t.route.routeId = :routeId
             AND t.departureTime >= :from
             AND t.departureTime < :to
           """)
    long calculateRouteCapacity(
            @Param("routeId") Long routeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
           SELECT COUNT(bs)
           FROM BookingSeat bs
           WHERE bs.trip.route.routeId = :routeId
             AND bs.booking.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
             AND bs.booking.bookingTime >= :from
             AND bs.booking.bookingTime < :to
           """)
    long calculateRouteSeatsSold(
            @Param("routeId") Long routeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}