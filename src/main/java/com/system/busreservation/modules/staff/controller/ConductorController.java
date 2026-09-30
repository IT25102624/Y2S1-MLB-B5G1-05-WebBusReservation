package com.system.busreservation.modules.staff.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.staff.dto.StaffDtos.BoardingRequest;
import com.system.busreservation.modules.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/conductor", "/api/conductor"})
@RequiredArgsConstructor
@Slf4j
public class ConductorController {

    private final StaffService staffService;

    @PostMapping("/board")
    public ResponseEntity<ApiResponse<Map<String, Object>>> board(
            @Valid @RequestBody BoardingRequest request) {

        log.info("Conductor verify & board request for reference: {}", request.bookingReference());
        Map<String, Object> result = staffService.boardPassenger(request.bookingReference(), request.remarks());
        return ResponseEntity.ok(ApiResponse.success("Passenger verified and boarded successfully", result));
    }
}
