package com.system.busreservation.modules.admin.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.admin.dto.FleetRequest;
import com.system.busreservation.modules.admin.dto.FleetResponse;
import com.system.busreservation.modules.admin.service.FleetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for fleet management, part of Use Case 6 ("execute Manage
 * Fleet & Routes" from the analytics dashboard).
 */
@RestController
@RequestMapping("/api/v1/admin/fleet")
@RequiredArgsConstructor
public class AdminFleetController {

    private final FleetService fleetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FleetResponse>>> getAllBuses() {
        return ResponseEntity.ok(ApiResponse.success("Fleet fetched", fleetService.getAllBuses()));
    }

    @GetMapping("/{busId}")
    public ResponseEntity<ApiResponse<FleetResponse>> getBus(@PathVariable Long busId) {
        return ResponseEntity.ok(ApiResponse.success("Bus fetched", fleetService.getBusById(busId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FleetResponse>> createBus(@Valid @RequestBody FleetRequest request) {
        FleetResponse created = fleetService.createBus(request);
        return new ResponseEntity<>(ApiResponse.success("Bus added to fleet", created), HttpStatus.CREATED);
    }

    @PutMapping("/{busId}")
    public ResponseEntity<ApiResponse<FleetResponse>> updateBus(@PathVariable Long busId,
                                                                @Valid @RequestBody FleetRequest request) {
        FleetResponse updated = fleetService.updateBus(busId, request);
        return ResponseEntity.ok(ApiResponse.success("Bus updated", updated));
    }

    @DeleteMapping("/{busId}")
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable Long busId) {
        fleetService.deleteBus(busId);
        return ResponseEntity.ok(ApiResponse.success("Bus removed from fleet", null));
    }
}
