package com.system.busreservation.modules.auth.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.model.enums.UserRole;
import com.system.busreservation.modules.auth.dto.AdminCreateUserRequest;
import com.system.busreservation.modules.auth.dto.AdminDashboardStats;
import com.system.busreservation.modules.auth.dto.AdminUpdateUserRequest;
import com.system.busreservation.modules.auth.dto.AuthResponse;
import com.system.busreservation.modules.auth.dto.ChangePasswordRequest;
import com.system.busreservation.modules.auth.dto.LoginRequest;
import com.system.busreservation.modules.auth.dto.RegisterRequest;
import com.system.busreservation.modules.auth.dto.StaffTripSummary;
import com.system.busreservation.modules.auth.dto.UpdateProfileRequest;
import com.system.busreservation.modules.auth.dto.UserResponse;
import com.system.busreservation.modules.auth.service.AuthService;
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
 * REST controller for Identity, Authentication, Role Management, and Conductor/Staff Portal.
 * Handles both public authentication endpoints (/api/v1/auth) and admin management endpoints (/api/v1/admin).
 */
@RestController
@RequestMapping({"/auth", "/api/v1/auth", "/admin", "/api/v1/admin"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to authenticate user: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User authenticated successfully", response));
    }

    @PostMapping({"/staff/login", "/api/v1/auth/staff/login", "/api/auth/staff/login"})
    public ResponseEntity<ApiResponse<AuthResponse>> staffLogin(@RequestBody java.util.Map<String, String> request) {
        String qrToken = request.get("qrToken");
        String email = request.get("email");
        String password = request.get("password");

        if (qrToken != null && !qrToken.isBlank()) {
            log.info("REST request to authenticate staff via QR token: {}", qrToken);
            AuthResponse response = authService.loginWithQr(qrToken.trim());
            return ResponseEntity.ok(ApiResponse.success("Staff authenticated via QR successfully", response));
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email != null ? email.trim() : "")
                .password(password != null ? password : "")
                .build();
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff authenticated successfully", response));
    }

    @PostMapping({"/admin/login", "/api/v1/auth/admin/login", "/api/auth/admin/login"})
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        return login(request);
    }

    @PostMapping({"/passenger/login", "/api/v1/auth/passenger/login", "/api/auth/passenger/login"})
    public ResponseEntity<ApiResponse<AuthResponse>> passengerLogin(@Valid @RequestBody LoginRequest request) {
        return login(request);
    }

    @PostMapping({"/passenger/register", "/api/v1/auth/passenger/register", "/api/auth/passenger/register"})
    public ResponseEntity<ApiResponse<AuthResponse>> passengerRegister(@Valid @RequestBody RegisterRequest request) {
        return register(request);
    }

    @GetMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            Authentication authentication,
            @RequestParam(required = false) String email) {

        String userEmail = resolveEmail(authentication, email);
        UserResponse user = authService.getCurrentUserProfile(userEmail);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @RequestParam(required = false) String email,
            @Valid @RequestBody UpdateProfileRequest request) {

        String userEmail = resolveEmail(authentication, email);
        UserResponse updatedUser = authService.updateProfile(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @RequestParam(required = false) String email,
            @Valid @RequestBody ChangePasswordRequest request) {

        String userEmail = resolveEmail(authentication, email);
        authService.changePassword(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats() {
        log.info("REST request to fetch admin dashboard stats");
        AdminDashboardStats stats = authService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/staff/trips")
    public ResponseEntity<ApiResponse<List<StaffTripSummary>>> getStaffTrips() {
        log.info("REST request to fetch conductor/driver trip overview");
        List<StaffTripSummary> trips = authService.getStaffTrips();
        return ResponseEntity.ok(ApiResponse.success(String.format("Found %d trips", trips.size()), trips));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role) {
        log.info("REST request to search users with keyword: '{}', role: '{}'", keyword, role);
        List<UserResponse> users = authService.getAllUsers(keyword, role);
        return ResponseEntity.ok(ApiResponse.success(String.format("Found %d users", users.size()), users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("REST request to get user details for ID: {}", id);
        UserResponse user = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", user));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        log.info("REST request by admin to create user: {}", request.getEmail());
        UserResponse createdUser = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully by admin", createdUser));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        log.info("REST request by admin to update user ID: {}", id);
        UserResponse updatedUser = authService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String adminEmail) {
        String currentAdmin = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())
                ? authentication.getName()
                : adminEmail;
        log.info("REST request by admin {} to delete user ID: {}", currentAdmin, id);
        authService.deleteUser(id, currentAdmin);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    private String resolveEmail(Authentication authentication, String email) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        throw new IllegalArgumentException("User email or authentication required");
    }
}
