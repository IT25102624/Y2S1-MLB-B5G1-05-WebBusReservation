package com.system.busreservation.modules.seat.repository;

import com.system.busreservation.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBus_BusId(Long busId);
    Optional<Seat> findByBus_BusIdAndSeatNumber(Long busId, String seatNumber);
}
