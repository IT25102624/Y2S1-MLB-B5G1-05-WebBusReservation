package com.busticket.repository;

import com.busticket.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

    List<SeatLock> findByTripId(Long tripId);

    List<SeatLock> findByTripIdAndSessionId(Long tripId, String sessionId);

    Optional<SeatLock> findByTripIdAndSeatId(Long tripId, Long seatId);

    List<SeatLock> findByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SeatLock sl WHERE sl.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SeatLock sl WHERE sl.trip.id = :tripId AND sl.seat.id IN :seatIds AND sl.sessionId = :sessionId")
    int deleteByTripIdAndSeatIdInAndSessionId(@Param("tripId") Long tripId, @Param("seatIds") List<Long> seatIds, @Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM SeatLock sl WHERE sl.trip.id = :tripId AND sl.sessionId = :sessionId")
    int deleteByTripIdAndSessionId(@Param("tripId") Long tripId, @Param("sessionId") String sessionId);
}
