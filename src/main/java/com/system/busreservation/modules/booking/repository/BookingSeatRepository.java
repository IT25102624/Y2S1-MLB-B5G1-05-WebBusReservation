package com.system.busreservation.modules.booking.repository;

import com.system.busreservation.model.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByTrip_TripId(Long tripId);
    List<BookingSeat> findByBooking_BookingId(Long bookingId);
    boolean existsByTrip_TripIdAndSeat_SeatId(Long tripId, Long seatId);
}
