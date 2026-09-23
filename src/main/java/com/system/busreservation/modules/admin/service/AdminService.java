package com.system.busreservation.modules.admin.service;

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

import java.util.List;

/**
 * Service for account sign-in, registration, user management, and the staff/admin portal.
 */
public interface AdminService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUserProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    AdminDashboardStats getDashboardStats();

    List<UserResponse> getAllUsers(String keyword, UserRole role);

    UserResponse getUserById(Long userId);

    UserResponse createUser(AdminCreateUserRequest request);

    UserResponse updateUser(Long userId, AdminUpdateUserRequest request);

    void deleteUser(Long userId, String currentAdminEmail);

    List<StaffTripSummary> getStaffTrips();
}
