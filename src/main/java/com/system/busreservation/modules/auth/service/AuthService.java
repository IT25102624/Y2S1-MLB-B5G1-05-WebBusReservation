package com.system.busreservation.modules.auth.service;

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

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithQr(String qrToken);

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
