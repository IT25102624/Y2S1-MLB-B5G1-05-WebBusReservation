package com.system.busreservation.modules.booking.repository;

import com.system.busreservation.model.Booking;
import com.system.busreservation.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser_UserId(Long userId);
    List<Booking> findByTrip_TripId(Long tripId);
    List<Booking> findByBookingStatus(BookingStatus status);
}
