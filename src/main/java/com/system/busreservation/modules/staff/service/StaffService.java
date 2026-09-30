package com.system.busreservation.modules.staff.service;

import com.system.busreservation.modules.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;

    public Map<String, Object> getDashboard(long userId) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> profile = staffRepository.findStaffProfile(userId).orElse(Map.of("employeeId", "EMP-" + userId, "active", true));
        response.put("profile", profile);

        var assignmentOpt = staffRepository.findCurrentAssignment(userId);
        if (assignmentOpt.isPresent()) {
            Map<String, Object> currentTrip = assignmentOpt.get();
            response.put("assignedTrip", currentTrip);
            long currentTripId = ((Number) currentTrip.get("tripId")).longValue();

            staffRepository.findPreviousTrip(currentTripId).ifPresent(prevTrip -> {
                Map<String, Object> precedingData = new HashMap<>(prevTrip);
                long prevTripId = ((Number) prevTrip.get("tripId")).longValue();
                staffRepository.findLatestLocation(prevTripId).ifPresent(loc -> precedingData.put("latestLocation", loc));
                response.put("precedingBus", precedingData);
            });
        } else {
            response.put("assignedTrip", null);
            response.put("precedingBus", null);
        }

        response.put("messages", staffRepository.listMessages(userId, false));
        return response;
    }

    @Transactional
    public void recordLocation(long userId, BigDecimal lat, BigDecimal lon, BigDecimal speed, BigDecimal heading) {
        var assignmentOpt = staffRepository.findCurrentAssignment(userId);
        if (assignmentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active trip assignment found for staff member.");
        }
        long tripId = ((Number) assignmentOpt.get().get("tripId")).longValue();
        staffRepository.saveLocation(tripId, lat, lon, speed, heading);
        log.info("Saved location update for trip ID {}: lat={}, lon={}", tripId, lat, lon);
    }

    @Transactional
    public Map<String, Object> boardPassenger(String reference, String remarks) {
        if (reference == null || reference.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scan or enter a valid passenger booking reference.");
        }

        String digits = reference.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking reference format.");
        }

        long bookingId;
        try {
            bookingId = Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking reference numeric ID.");
        }

        var bookingOpt = staffRepository.findBooking(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unrecognized booking reference: " + reference);
        }

        Map<String, Object> booking = bookingOpt.get();
        String currentStatus = (String) booking.get("status");

        if ("CANCELLED".equalsIgnoreCase(currentStatus) || "CANCELED".equalsIgnoreCase(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.GONE, "This ticket has been cancelled and cannot be boarded.");
        }
        if ("BOARDED".equalsIgnoreCase(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate scan: passenger has already boarded.");
        }

        staffRepository.updateBookingStatus(bookingId, "BOARDED");
        long tripId = ((Number) booking.get("tripId")).longValue();
        int boardedCount = staffRepository.countBoarded(tripId);

        log.info("Passenger booking {} marked as BOARDED for trip {}", reference, tripId);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingReference", reference);
        result.put("bookingId", bookingId);
        result.put("tripId", tripId);
        result.put("boardedCount", boardedCount);
        result.put("remarks", remarks != null ? remarks.trim() : "");
        result.put("verifiedAt", Instant.now());
        return result;
    }

    public void postMessage(long userId, String subject, String message, boolean urgent) {
        staffRepository.saveMessage(userId, subject, message, urgent);
    }

    public List<Map<String, Object>> getMessages(long userId, boolean isAdmin) {
        return staffRepository.listMessages(userId, isAdmin);
    }
}
