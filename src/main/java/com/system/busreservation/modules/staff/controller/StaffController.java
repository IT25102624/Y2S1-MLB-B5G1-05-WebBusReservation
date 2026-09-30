package com.system.busreservation.modules.staff.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.auth.repository.UserRepository;
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

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long userId) {

        long resolvedId = resolveUserId(authentication, userId);
        Map<String, Object> dashboard = staffService.getDashboard(resolvedId);
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

    private long resolveUserId(Authentication authentication, Long explicitId) {
        if (explicitId != null && explicitId > 0) return explicitId;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String email = authentication.getName();
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) return userOpt.get().getUserId();
        }
        return 1L; // Fallback to primary staff user
    }
}
