package com.system.busreservation.modules.admin.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.model.enums.UserRole;
import com.system.busreservation.modules.admin.dto.AdminCreateUserRequest;
import com.system.busreservation.modules.admin.dto.AdminDashboardStats;
import com.system.busreservation.modules.admin.dto.AdminUpdateUserRequest;
import com.system.busreservation.modules.admin.dto.AuthResponse;
import com.system.busreservation.modules.admin.dto.ChangePasswordRequest;
import com.system.busreservation.modules.admin.dto.LoginRequest;
import com.system.busreservation.modules.admin.dto.RegisterRequest;
import com.system.busreservation.modules.admin.dto.StaffTripSummary;
import com.system.busreservation.modules.admin.dto.UpdateProfileRequest;
import com.system.busreservation.modules.admin.dto.UserResponse;
import com.system.busreservation.modules.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Identity, Role Management, and the Admin/Staff portal.
 * Public sign-in and registration live under /api/v1/admin and remain available
 * at /api/v1/auth for existing links.
 */
@RestController
@RequestMapping({"/admin", "/api/v1/admin", "/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {}", request.getEmail());
        AuthResponse response = adminService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to authenticate user: {}", request.getEmail());
        AuthResponse response = adminService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User authenticated successfully", response));
    }

    @GetMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            Authentication authentication,
            @RequestParam(required = false) String email) {

        String userEmail = resolveEmail(authentication, email);
        UserResponse user = adminService.getCurrentUserProfile(userEmail);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @RequestParam(required = false) String email,
            @Valid @RequestBody UpdateProfileRequest request) {

        String userEmail = resolveEmail(authentication, email);
        UserResponse updatedUser = adminService.updateProfile(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @RequestParam(required = false) String email,
            @Valid @RequestBody ChangePasswordRequest request) {

        String userEmail = resolveEmail(authentication, email);
        adminService.changePassword(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats() {
        log.info("REST request to fetch admin dashboard stats");
        AdminDashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/staff/trips")
    public ResponseEntity<ApiResponse<List<StaffTripSummary>>> getStaffTrips() {
        log.info("REST request to fetch conductor/driver trip overview");
        List<StaffTripSummary> trips = adminService.getStaffTrips();
        return ResponseEntity.ok(ApiResponse.success(String.format("Found %d trips", trips.size()), trips));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role) {
        log.info("REST request to search users with keyword: '{}', role: '{}'", keyword, role);
        List<UserResponse> users = adminService.getAllUsers(keyword, role);
        return ResponseEntity.ok(ApiResponse.success(String.format("Found %d users", users.size()), users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("REST request to get user details for ID: {}", id);
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", user));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        log.info("REST request by admin to create user: {}", request.getEmail());
        UserResponse createdUser = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully by admin", createdUser));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        log.info("REST request by admin to update user ID: {}", id);
        UserResponse updatedUser = adminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String adminEmail) {
        String currentAdmin = authentication != null ? authentication.getName() : adminEmail;
        log.info("REST request by admin {} to delete user ID: {}", currentAdmin, id);
        adminService.deleteUser(id, currentAdmin);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    private String resolveEmail(Authentication authentication, String email) {
        String userEmail = authentication != null ? authentication.getName() : email;
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email or authentication required");
        }
        return userEmail;
    }
}
