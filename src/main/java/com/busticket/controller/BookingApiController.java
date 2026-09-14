package com.busticket.controller;

import com.busticket.dto.*;
import com.busticket.model.*;
import com.busticket.model.enums.BookingStatus;
import com.busticket.model.enums.SeatStatus;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.SeatLockRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.TripRepository;
import com.busticket.service.BookingService;
import com.busticket.service.FareCalculatorService;
import com.busticket.service.SeatLockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BookingApiController {

    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final FareCalculatorService fareCalculatorService;
    private final BookingService bookingService;

    public BookingApiController(TripRepository tripRepository,
                                SeatRepository seatRepository,
                                SeatLockRepository seatLockRepository,
                                BookingRepository bookingRepository,
                                SeatLockService seatLockService,
                                FareCalculatorService fareCalculatorService,
                                BookingService bookingService) {
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
        this.fareCalculatorService = fareCalculatorService;
        this.bookingService = bookingService;
    }

    @GetMapping("/trips")
    public ResponseEntity<List<Trip>> getAllTrips() {
        return ResponseEntity.ok(tripRepository.findAll());
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trips/{id}/seats")
    public ResponseEntity<List<SeatMapResponseDto>> getSeatsForTrip(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String sessionId) {

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + id));

        List<Seat> busSeats = seatRepository.findByBus(trip.getBus());

        List<SeatLock> activeLocks = seatLockService.getActiveLocksForTrip(id);
        Map<Long, SeatLock> lockMapBySeatId = activeLocks.stream()
                .filter(l -> !l.isExpired())
                .collect(Collectors.toMap(l -> l.getSeat().getId(), l -> l, (l1, l2) -> l1));

        List<Booking> confirmedBookings = bookingRepository.findByTripId(id).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());

        Set<String> confirmedBookedSeatNumbers = new HashSet<>();
        for (Booking booking : confirmedBookings) {
            confirmedBookedSeatNumbers.addAll(booking.getSeatNumbers());
        }

        double basePrice = trip.getBasePrice();

        List<SeatMapResponseDto> responseList = busSeats.stream().map(seat -> {
            SeatMapResponseDto dto = new SeatMapResponseDto();
            dto.setId(seat.getId());
            dto.setSeatNumber(seat.getSeatNumber());
            dto.setDeckType(seat.getDeckType());
            dto.setRowNum(seat.getRowNum());
            dto.setColNum(seat.getColNum());
            dto.setSeatType(seat.getSeatType());
            dto.setSeatTypeDisplayName(seat.getSeatType().getDisplayName());
            dto.setCalculatedPrice(basePrice + seat.getSeatType().getPriceSurcharge());

            if (confirmedBookedSeatNumbers.contains(seat.getSeatNumber())) {
                dto.setStatus(SeatStatus.BOOKED);
                dto.setLockedByMe(false);
            } else if (lockMapBySeatId.containsKey(seat.getId())) {
                SeatLock lock = lockMapBySeatId.get(seat.getId());
                dto.setStatus(SeatStatus.LOCKED);
                dto.setLockedByMe(lock.getSessionId().equals(sessionId));
            } else {
                dto.setStatus(SeatStatus.AVAILABLE);
                dto.setLockedByMe(false);
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/seats/lock")
    public ResponseEntity<LockSeatResponse> lockSeats(@RequestBody LockSeatRequest request) {
        try {
            LockSeatResponse response = seatLockService.tryLockSeats(request.getTripId(), request.getSeatIds(), request.getSessionId());
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LockSeatResponse(false, e.getMessage(), Collections.emptyList(), null, 0));
        }
    }

    @PostMapping("/seats/unlock")
    public ResponseEntity<Map<String, Object>> unlockSeats(@RequestBody LockSeatRequest request) {
        seatLockService.releaseSeats(request.getTripId(), request.getSeatIds(), request.getSessionId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Seats unlocked successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/fare/calculate")
    public ResponseEntity<FareBreakdownDto> calculateFare(
            @RequestParam Long tripId,
            @RequestBody List<Long> seatIds) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        List<Seat> seats = seatRepository.findAllById(seatIds);
        FareBreakdownDto breakdown = fareCalculatorService.calculateFare(trip, seats);
        return ResponseEntity.ok(breakdown);
    }

    @PostMapping("/bookings/confirm")
    public ResponseEntity<?> confirmBooking(@RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.confirmBooking(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pnr", booking.getPnr());
            response.put("message", "Booking confirmed successfully!");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Booking failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
    @PostMapping("/bookings/{pnr}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String pnr) {
        try {
            bookingService.cancelBooking(pnr);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking cancelled successfully!");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/bookings/{pnr}")
    public ResponseEntity<?> updateBooking(@PathVariable String pnr, @RequestBody UpdateBookingRequest request) {
        try {
            Booking updatedBooking = bookingService.updateBooking(pnr, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking updated successfully!");
            response.put("pnr", updatedBooking.getPnr());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
