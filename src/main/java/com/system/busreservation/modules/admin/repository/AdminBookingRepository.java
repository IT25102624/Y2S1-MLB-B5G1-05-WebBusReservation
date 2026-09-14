package com.system.busreservation.modules.admin.repository;

import com.system.busreservation.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminBookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
           SELECT COALESCE(SUM(b.totalAmount), 0)
           FROM Booking b
           WHERE b.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
             AND b.bookingTime >= :from
             AND b.bookingTime < :to
             AND (:routeId IS NULL OR b.trip.route.routeId = :routeId)
             AND (:busId IS NULL OR b.trip.bus.busId = :busId)
           """)
    BigDecimal sumRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("routeId") Long routeId,
            @Param("busId") Long busId
    );

    @Query("""
           SELECT COUNT(b)
           FROM Booking b
           WHERE b.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
             AND b.bookingTime >= :from
             AND b.bookingTime < :to
             AND (:routeId IS NULL OR b.trip.route.routeId = :routeId)
             AND (:busId IS NULL OR b.trip.bus.busId = :busId)
           """)
    long countBookings(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("routeId") Long routeId,
            @Param("busId") Long busId
    );

    @Query("""
           SELECT COUNT(bs)
           FROM BookingSeat bs
           WHERE bs.booking.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
             AND bs.booking.bookingTime >= :from
             AND bs.booking.bookingTime < :to
             AND (:routeId IS NULL OR bs.booking.trip.route.routeId = :routeId)
             AND (:busId IS NULL OR bs.booking.trip.bus.busId = :busId)
           """)
    long countSeatsSold(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("routeId") Long routeId,
            @Param("busId") Long busId
    );

    @Query("""
           SELECT
               b.bookingTime,
               b.totalAmount,
               b.bookingId
           FROM Booking b
           WHERE b.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
             AND b.bookingTime >= :from
             AND b.bookingTime < :to
             AND (:routeId IS NULL OR b.trip.route.routeId = :routeId)
             AND (:busId IS NULL OR b.trip.bus.busId = :busId)
           ORDER BY b.bookingTime
           """)
    List<Object[]> dailyRevenueData(
            LocalDateTime from,
            LocalDateTime to,
            Long routeId,
            Long busId
    );
}