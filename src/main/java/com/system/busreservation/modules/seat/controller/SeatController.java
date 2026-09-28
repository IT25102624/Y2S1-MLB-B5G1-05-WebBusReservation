package com.system.busreservation.modules.seat.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller placeholder for Seat Selection and Real-Time Availability (Assigned to IT25101791).
 */
@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSeatMap(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success("Seat availability retrieved", seatService.getSeatMap(tripId)));
    }
}
