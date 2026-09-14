package com.system.busreservation.modules.booking.controller;

import com.system.busreservation.model.Booking;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Ticket Booking (Assigned to IT25101791 / IT25103561).
 * Includes passenger booking lookup for Module 5 luggage tagging integration.
 */
@RestController
@RequestMapping({"/bookings", "/api/v1/bookings", "/api/bookings"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingRepository bookingRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getBookingsForUser(@PathVariable String userId) {
        log.info("REST request to fetch bookings for user reference: {}", userId);

        Long numericUserId = null;
        try {
            numericUserId = Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
        }

        List<Booking> bookings;
        if (numericUserId != null) {
            bookings = bookingRepository.findByUser_UserId(numericUserId);
        } else {
            bookings = bookingRepository.findAll();
        }

        List<Map<String, Object>> response = bookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getBookingId());
            map.put("bookingTime", b.getBookingTime());
            map.put("status", b.getBookingStatus() != null ? b.getBookingStatus().name() : "CONFIRMED");
            map.put("totalAmount", b.getTotalAmount());
            if (b.getTrip() != null && b.getTrip().getRoute() != null) {
                map.put("route", b.getTrip().getRoute().getSourceCity() + " to " + b.getTrip().getRoute().getDestinationCity());
            } else {
                map.put("route", "Express Route");
            }
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }
}
