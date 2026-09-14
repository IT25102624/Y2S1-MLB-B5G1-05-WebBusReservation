package com.busticket.repository;

import com.busticket.model.Bus;
import com.busticket.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBus(Bus bus);
    List<Seat> findByBusId(Long busId);
    Optional<Seat> findByBusIdAndSeatNumber(Long busId, String seatNumber);
}
