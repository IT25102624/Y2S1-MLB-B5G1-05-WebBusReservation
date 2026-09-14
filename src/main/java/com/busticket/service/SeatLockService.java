package com.busticket.service;

import com.busticket.dto.LockSeatResponse;
import com.busticket.model.Seat;
import com.busticket.model.SeatLock;
import com.busticket.model.Trip;
import com.busticket.repository.SeatLockRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);

    private final SeatLockRepository seatLockRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;

    @Value("${app.seat-lock.duration-seconds:300}")
    private long lockDurationSeconds;

    // Concurrent in-memory mutex to ensure atomic lock evaluations
    private final ConcurrentHashMap<String, Object> tripLocks = new ConcurrentHashMap<>();

    public SeatLockService(SeatLockRepository seatLockRepository, SeatRepository seatRepository, TripRepository tripRepository) {
        this.seatLockRepository = seatLockRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public synchronized LockSeatResponse tryLockSeats(Long tripId, List<Long> seatIds, String sessionId) {
        // Clean up any stale locks first
        cleanupExpiredLocks();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + tripId));

        LocalDateTime now = LocalDateTime.now();
        List<SeatLock> existingLocksForTrip = seatLockRepository.findByTripId(tripId);

        // Check if any requested seat is already locked by someone else
        List<String> lockedByOthersSeats = new ArrayList<>();
        for (SeatLock lock : existingLocksForTrip) {
            if (seatIds.contains(lock.getSeat().getId())) {
                if (!lock.getSessionId().equals(sessionId) && lock.getExpiresAt().isAfter(now)) {
                    lockedByOthersSeats.add(lock.getSeat().getSeatNumber());
                }
            }
        }

        if (!lockedByOthersSeats.isEmpty()) {
            return new LockSeatResponse(
                    false,
                    "Seats [" + String.join(", ", lockedByOthersSeats) + "] are currently selected/locked by another user.",
                    Collections.emptyList(),
                    null,
                    0
            );
        }

        // Release previous locks held by this session for seats NOT in the current request list
        List<SeatLock> myCurrentLocks = seatLockRepository.findByTripIdAndSessionId(tripId, sessionId);
        for (SeatLock lock : myCurrentLocks) {
            if (!seatIds.contains(lock.getSeat().getId())) {
                seatLockRepository.delete(lock);
            }
        }

        // Lock requested seats
        LocalDateTime expiresAt = now.plusSeconds(lockDurationSeconds);
        List<Long> newlyLockedIds = new ArrayList<>();

        for (Long seatId : seatIds) {
            Optional<SeatLock> existingMyLock = seatLockRepository.findByTripIdAndSeatId(tripId, seatId);
            if (existingMyLock.isPresent()) {
                SeatLock lock = existingMyLock.get();
                lock.setExpiresAt(expiresAt);
                seatLockRepository.save(lock);
                newlyLockedIds.add(seatId);
            } else {
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("Seat not found with ID: " + seatId));
                SeatLock newLock = new SeatLock(trip, seat, sessionId, now, expiresAt);
                seatLockRepository.save(newLock);
                newlyLockedIds.add(seatId);
            }
        }

        long remainingSeconds = Duration.between(now, expiresAt).getSeconds();

        return new LockSeatResponse(
                true,
                "Seats locked successfully for " + (lockDurationSeconds / 60) + " minutes.",
                newlyLockedIds,
                expiresAt,
                Math.max(0, remainingSeconds)
        );
    }

    @Transactional
    public void releaseSeats(Long tripId, List<Long> seatIds, String sessionId) {
        if (seatIds == null || seatIds.isEmpty()) {
            seatLockRepository.deleteByTripIdAndSessionId(tripId, sessionId);
        } else {
            seatLockRepository.deleteByTripIdAndSeatIdInAndSessionId(tripId, seatIds, sessionId);
        }
    }

    @Transactional
    public List<SeatLock> getActiveLocksForTrip(Long tripId) {
        cleanupExpiredLocks();
        return seatLockRepository.findByTripId(tripId);
    }


    @Scheduled(fixedRate = 5000)
    @Transactional
    public void scheduledCleanupExpiredLocks() {
        cleanupExpiredLocks();
    }

    @Transactional
    public int cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = seatLockRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Cleaned up {} expired seat locks.", deleted);
        }
        return deleted;
    }
}
