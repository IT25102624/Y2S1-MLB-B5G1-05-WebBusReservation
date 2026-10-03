package com.system.busreservation.modules.staff.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.auth.dto.StaffTripSummary;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.auth.service.AuthService;
import com.system.busreservation.modules.staff.dto.StaffDtos.*;
import com.system.busreservation.modules.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/staff", "/api/staff"})
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService staffService;
    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<List<StaffTripSummary>>> getStaffTrips() {
        log.info("Fetching all trips for staff overview");
        List<StaffTripSummary> trips = authService.getStaffTrips();
        return ResponseEntity.ok(ApiResponse.success(String.format("Found %d trips", trips.size()), trips));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tripId) {

        long resolvedId = resolveUserId(authentication, userId);
        Map<String, Object> dashboard = staffService.getDashboard(resolvedId, tripId);
        return ResponseEntity.ok(ApiResponse.success("Staff dashboard retrieved successfully", dashboard));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            Authentication authentication,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody LocationUpdateRequest request) {

        long resolvedId = resolveUserId(authentication, userId);
        staffService.recordLocation(resolvedId, request.latitude(), request.longitude(),
                request.speedKmh(), request.headingDegrees());
        return ResponseEntity.ok(ApiResponse.success("Bus location update recorded successfully", null));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(
            Authentication authentication,
            @RequestParam(required = false) Long userId) {

        long resolvedId = resolveUserId(authentication, userId);
        List<Map<String, Object>> messages = staffService.getMessages(resolvedId, false);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Void>> postMessage(
            Authentication authentication,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody OperationsMessageRequest request) {

        long resolvedId = resolveUserId(authentication, userId);
        staffService.postMessage(resolvedId, request.subject(), request.message(), request.urgent());
        return ResponseEntity.ok(ApiResponse.success("Operations message logged successfully", null));
    }

    @PostMapping("/trips/{tripId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeTrip(
            Authentication authentication,
            @PathVariable Long tripId,
            @RequestParam(required = false) Long userId) {

        long resolvedId = resolveUserId(authentication, userId);
        log.info("Driver marking trip ID {} as completed (userId={})", tripId, resolvedId);
        Map<String, Object> result = staffService.completeTrip(tripId, resolvedId);
        return ResponseEntity.ok(ApiResponse.success("Trip marked as completed successfully", result));
    }

    @GetMapping("/trips/{tripId}/luggage")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTripLuggage(
            @PathVariable Long tripId) {

        log.info("Fetching baggage manifest for trip ID: {}", tripId);
        List<Map<String, Object>> luggage = staffService.getLuggageForTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip baggage inventory retrieved successfully", luggage));
    }

    @GetMapping({"/crew-roster", "/admin/crew-assignments"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCrewRoster() {
        log.info("Fetching highway trip crew roster");
        List<Map<String, Object>> roster = staffService.getTripsWithCrew();
        return ResponseEntity.ok(ApiResponse.success("Crew roster retrieved successfully", roster));
    }

    @GetMapping({"/crew-members", "/admin/crew-members"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCrewMembers() {
        log.info("Fetching registered drivers and conductors for assignment");
        List<Map<String, Object>> members = staffService.getCrewMembers();
        return ResponseEntity.ok(ApiResponse.success("Available crew members retrieved successfully", members));
    }

    public record AssignCrewRequest(Long tripId, Long driverUserId, Long conductorUserId) {}

    @PostMapping({"/crew-assign", "/admin/crew-assignments"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignCrew(
            Authentication authentication,
            @RequestParam(required = false) Long adminUserId,
            @RequestBody AssignCrewRequest request) {

        long resolvedAdminId = resolveUserId(authentication, adminUserId);
        log.info("Administrator ID {} assigning crew to trip {}: driver={}, conductor={}",
                resolvedAdminId, request.tripId(), request.driverUserId(), request.conductorUserId());
        staffService.assignCrew(request.tripId(), request.driverUserId(), request.conductorUserId(), resolvedAdminId);
        return ResponseEntity.ok(ApiResponse.success("Crew assigned successfully to Trip #" + request.tripId(),
                Map.of("tripId", request.tripId(), "assigned", true)));
    }

    private long resolveUserId(Authentication authentication, Long explicitId) {
        if (explicitId != null && explicitId > 0) return explicitId;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String email = authentication.getName();
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) return userOpt.get().getUserId();
        }
        return userRepository.findAll().stream().findFirst().map(u -> u.getUserId()).orElse(1L);
    }
}
