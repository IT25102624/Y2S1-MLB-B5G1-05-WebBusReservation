package com.system.busreservation.modules.booking.repository;

import com.system.busreservation.model.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByTrip_TripId(Long tripId);
    List<BookingSeat> findByBooking_BookingId(Long bookingId);
    boolean existsByTrip_TripIdAndSeat_SeatId(Long tripId, Long seatId);

    @Query("""
        SELECT COUNT(bs.bookingSeatId)
        FROM BookingSeat bs
        WHERE bs.trip.tripId = :tripId
          AND bs.booking.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
    """)
    int countActiveBookedSeatsByTripId(@Param("tripId") Long tripId);

    @Query("""
        SELECT bs.trip.tripId, COUNT(bs.bookingSeatId)
        FROM BookingSeat bs
        WHERE bs.trip.tripId IN :tripIds
          AND bs.booking.bookingStatus <> com.system.busreservation.model.enums.BookingStatus.CANCELLED
        GROUP BY bs.trip.tripId
    """)
    List<Object[]> countActiveBookedSeatsByTripIds(@Param("tripIds") List<Long> tripIds);
}
