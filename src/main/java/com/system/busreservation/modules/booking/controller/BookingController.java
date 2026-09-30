package com.system.busreservation.modules.booking.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/bookings", "/api/bookings"})
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookings(
            @RequestParam(required = false) String email) {
        log.info("REST request to retrieve bookings for email: {}", email);
        List<Map<String, Object>> bookings = bookingService.getBookingsForUser(email);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(@RequestBody Map<String, Object> request) {
        log.info("REST request to create seat booking: {}", request);
        Map<String, Object> booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBooking(@PathVariable String reference) {
        log.info("REST request to retrieve booking: {}", reference);
        Map<String, Object> booking = bookingService.getBooking(reference);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    @DeleteMapping("/{reference}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(@PathVariable String reference) {
        log.info("REST request to cancel booking: {}", reference);
        Map<String, Object> cancelled = bookingService.cancelBooking(reference);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", cancelled));
    }
}
