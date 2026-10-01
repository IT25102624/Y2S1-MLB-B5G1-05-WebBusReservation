package com.system.busreservation.modules.luggage.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.model.enums.LuggageStatus;
import com.system.busreservation.modules.luggage.dto.LuggageCreateRequest;
import com.system.busreservation.modules.luggage.dto.LuggageResponse;
import com.system.busreservation.modules.luggage.dto.LuggageStatusUpdateRequest;
import com.system.busreservation.modules.luggage.service.LuggageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Luggage Tracking and Tagging.
 * Module 5: Assigned to Wanniarachchi P. S. (IT25103670).
 */
@RestController
@RequestMapping({"/luggage", "/api/v1/luggage", "/api/luggage"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class LuggageController {

    private final LuggageService luggageService;

    @GetMapping
    public ResponseEntity<List<LuggageResponse>> getAllLuggage() {
        log.info("REST request to list all luggage");
        return ResponseEntity.ok(luggageService.getAllLuggage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LuggageResponse> getLuggageById(@PathVariable Long id) {
        log.info("REST request to fetch luggage by ID: {}", id);
        return ResponseEntity.ok(luggageService.getLuggageById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LuggageResponse>> getLuggageByUser(@PathVariable Long userId) {
        log.info("REST request to fetch luggage for user ID: {}", userId);
        return ResponseEntity.ok(luggageService.getLuggageByUserId(userId));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<LuggageResponse>> getLuggageByBooking(@PathVariable Long bookingId) {
        log.info("REST request to fetch luggage for booking ID: {}", bookingId);
        return ResponseEntity.ok(luggageService.getLuggageByBookingId(bookingId));
    }

    @GetMapping("/tag/{tagNumber}")
    public ResponseEntity<LuggageResponse> getLuggageByTag(@PathVariable String tagNumber) {
        log.info("REST request to fetch luggage by tag: {}", tagNumber);
        return ResponseEntity.ok(luggageService.getLuggageByTagNumber(tagNumber));
    }

    @PostMapping
    public ResponseEntity<LuggageResponse> createLuggage(@Valid @RequestBody LuggageCreateRequest request) {
        log.info("REST request to create luggage for booking: {}", request.getBookingId());
        LuggageResponse created = luggageService.createLuggage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<LuggageResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String statusStr = request.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LuggageStatus status = LuggageStatus.valueOf(statusStr.trim().toUpperCase());
        LuggageStatusUpdateRequest updateRequest = new LuggageStatusUpdateRequest(status);
        log.info("REST request to update luggage ID {} status to {}", id, status);
        return ResponseEntity.ok(luggageService.updateStatus(id, updateRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LuggageResponse> updateLuggage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("REST request to update luggage record ID: {}", id);
        Integer bagQuantity = body.get("bagQuantity") != null ? Integer.parseInt(body.get("bagQuantity").toString()) : null;
        String description = (String) body.get("description");
        LuggageStatus status = null;
        if (body.get("status") != null && !body.get("status").toString().isBlank()) {
            status = LuggageStatus.valueOf(body.get("status").toString().trim().toUpperCase());
        }
        return ResponseEntity.ok(luggageService.updateLuggage(id, bagQuantity, description, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLuggage(@PathVariable Long id) {
        log.info("REST request to delete luggage ID: {}", id);
        luggageService.deleteLuggage(id);
        return ResponseEntity.ok(ApiResponse.success("Luggage record deleted successfully", null));
    }
}
