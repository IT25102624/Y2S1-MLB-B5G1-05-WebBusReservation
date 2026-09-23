package com.system.busreservation.modules.admin.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.User;
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
import com.system.busreservation.modules.admin.repository.UserRepository;
import com.system.busreservation.modules.admin.security.JwtTokenProvider;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Handles public sign-in and registration, admin user management, and staff trip overviews.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private static final EnumSet<UserRole> SELF_REGISTER_ROLES =
            EnumSet.of(UserRole.PASSENGER, UserRole.CONDUCTOR, UserRole.DRIVER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.PASSENGER;
        if (!SELF_REGISTER_ROLES.contains(role)) {
            throw new IllegalArgumentException("Administrator accounts can only be created from the admin portal");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {} and role: {}", savedUser.getUserId(), savedUser.getRole());

        return toAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Processing user login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User {} authenticated successfully with role {}", user.getEmail(), user.getRole());
        return toAuthResponse(user);
    }

    @Override
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        log.info("Updating user profile for email: {}", email);

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Processing password change for user: {}", email);

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password successfully updated for user: {}", email);
    }

    @Override
    public AdminDashboardStats getDashboardStats() {
        log.info("Fetching admin dashboard metrics");

        long totalUsers = userRepository.count();
        long totalPassengers = userRepository.countByRole(UserRole.PASSENGER);
        long totalConductors = userRepository.countByRole(UserRole.CONDUCTOR);
        long totalDrivers = userRepository.countByRole(UserRole.DRIVER);
        long totalAdmins = userRepository.countByRole(UserRole.ADMIN);

        List<UserResponse> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();

        return AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .totalPassengers(totalPassengers)
                .totalConductors(totalConductors)
                .totalDrivers(totalDrivers)
                .totalAdmins(totalAdmins)
                .recentUsers(recentUsers)
                .build();
    }

    @Override
    public List<UserResponse> getAllUsers(String keyword, UserRole role) {
        log.info("Fetching all users with keyword: '{}', role: '{}'", keyword, role);

        List<User> users;
        if ((keyword != null && !keyword.isBlank()) || role != null) {
            users = userRepository.searchUsers(keyword != null ? keyword.trim() : null, role);
        } else {
            users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return users.stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user details for ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        log.info("Admin creating new user with email: {} and role: {}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin successfully created user with ID: {}", savedUser.getUserId());

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        log.info("Admin updating user ID: {} with role: {}", userId, request.getRole());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        log.info("User ID: {} successfully updated by Admin", userId);

        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, String currentAdminEmail) {
        log.info("Admin request to delete user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (currentAdminEmail != null && user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("You cannot delete your own active administrator account");
        }

        userRepository.delete(user);
        log.info("User ID: {} successfully deleted by Admin", userId);
    }

    @Override
    public List<StaffTripSummary> getStaffTrips() {
        log.info("Fetching trip overview for conductor/driver portal");

        List<Trip> trips = tripRepository.findAllWithDetails();
        return trips.stream()
                .map(trip -> StaffTripSummary.builder()
                        .tripId(trip.getTripId())
                        .busNumber(trip.getBus().getBusNumber())
                        .operatorName(trip.getBus().getOperatorName())
                        .sourceCity(trip.getRoute().getSourceCity())
                        .destinationCity(trip.getRoute().getDestinationCity())
                        .departureTime(trip.getDepartureTime())
                        .arrivalTime(trip.getArrivalTime())
                        .baseFare(trip.getBaseFare())
                        .tripStatus(trip.getTripStatus() != null ? trip.getTripStatus().name() : null)
                        .bookedPassengerCount(bookingRepository.findByTrip_TripId(trip.getTripId()).size())
                        .build())
                .toList();
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(user))
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
