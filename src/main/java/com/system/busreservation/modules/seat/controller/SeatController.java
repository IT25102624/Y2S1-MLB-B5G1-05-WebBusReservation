package com.system.busreservation.modules.seat.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/seats", "/api/seats"})
@RequiredArgsConstructor
@Slf4j
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/layout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLayout(
            @RequestParam(required = false, defaultValue = "NON_AC_NORMAL") String busType) {
        log.info("Fetching seat layout for bus type: {}", busType);
        return ResponseEntity.ok(ApiResponse.success("Seat layout retrieved successfully", seatService.getSeatLayout(busType)));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTripSeats(
            @PathVariable Long tripId,
            @RequestParam(required = false, defaultValue = "NON_AC_NORMAL") String busType) {
        log.info("Fetching seat availability for trip ID: {}, busType: {}", tripId, busType);
        return ResponseEntity.ok(ApiResponse.success("Trip seats retrieved successfully", seatService.getTripSeats(tripId, busType)));
    }
}
