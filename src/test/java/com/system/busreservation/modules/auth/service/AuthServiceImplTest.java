package com.system.busreservation.modules.auth.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Bus;
import com.system.busreservation.model.Route;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.User;
import com.system.busreservation.model.enums.TripStatus;
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
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.auth.security.JwtTokenProvider;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .phoneNumber("+94771234567")
                .passwordHash("encoded_pwd")
                .role(UserRole.PASSENGER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phoneNumber("+94771234567")
                .password("secret123")
                .role(UserRole.PASSENGER)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_pwd");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token-xyz");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.register(req);

        assertThat(res).isNotNull();
        assertThat(res.getAccessToken()).isEqualTo("jwt-token-xyz");
        assertThat(res.getUser().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Duplicate Email Throws Exception")
    void register_DuplicateEmail() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phoneNumber("+94771234567")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Register - Cannot self-register as ADMIN")
    void register_DisallowAdminSelfRegister() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Admin Guy")
                .email("admin@example.com")
                .phoneNumber("+94771234567")
                .password("secret123")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator accounts can only be created");
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        LoginRequest req = LoginRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "encoded_pwd")).thenReturn(true);
        when(jwtTokenProvider.generateToken(sampleUser)).thenReturn("valid-jwt");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.login(req);

        assertThat(res.getAccessToken()).isEqualTo("valid-jwt");
        assertThat(res.getUser().getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Login - Bad Password Throws BadCredentialsException")
    void login_BadPassword() {
        LoginRequest req = LoginRequest.builder()
                .email("john@example.com")
                .password("wrong_password")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_password", "encoded_pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Get Profile - User Found")
    void getCurrentUserProfile_Found() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse res = authService.getCurrentUserProfile("john@example.com");

        assertThat(res.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Get Profile - User Not Found")
    void getCurrentUserProfile_NotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUserProfile("notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update Profile - Success")
    void updateProfile_Success() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .phoneNumber("+94719999999")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = authService.updateProfile("john@example.com", req);

        assertThat(res.getFullName()).isEqualTo("John Updated");
        assertThat(res.getPhoneNumber()).isEqualTo("+94719999999");
    }

    @Test
    @DisplayName("Change Password - Success")
    void changePassword_Success() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("current_pwd")
                .newPassword("new_pwd_123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("current_pwd", "encoded_pwd")).thenReturn(true);
        when(passwordEncoder.matches("new_pwd_123", "encoded_pwd")).thenReturn(false);
        when(passwordEncoder.encode("new_pwd_123")).thenReturn("new_encoded_pwd");

        authService.changePassword("john@example.com", req);

        verify(userRepository).save(sampleUser);
        assertThat(sampleUser.getPasswordHash()).isEqualTo("new_encoded_pwd");
    }

    @Test
    @DisplayName("Get Dashboard Stats")
    void getDashboardStats_ReturnsCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRole(UserRole.PASSENGER)).thenReturn(7L);
        when(userRepository.countByRole(UserRole.CONDUCTOR)).thenReturn(1L);
        when(userRepository.countByRole(UserRole.DRIVER)).thenReturn(1L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(sampleUser));

        AdminDashboardStats stats = authService.getDashboardStats();

        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getTotalPassengers()).isEqualTo(7L);
        assertThat(stats.getRecentUsers()).hasSize(1);
    }

    @Test
    @DisplayName("Admin Create User - Success")
    void createUser_Success() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .fullName("Conductor Perera")
                .email("conductor@example.com")
                .phoneNumber("+94762222222")
                .password("pwd12345")
                .role(UserRole.CONDUCTOR)
                .build();

        when(userRepository.existsByEmail("conductor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pwd12345")).thenReturn("enc_pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(2L);
            return u;
        });

        UserResponse res = authService.createUser(req);

        assertThat(res.getUserId()).isEqualTo(2L);
        assertThat(res.getRole()).isEqualTo(UserRole.CONDUCTOR);
    }

    @Test
    @DisplayName("Delete User - Admin cannot delete self")
    void deleteUser_PreventSelfDeletion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.deleteUser(1L, "john@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot delete your own active administrator account");
    }

    @Test
    @DisplayName("Staff Trips - Return Summaries")
    void getStaffTrips_ReturnsList() {
        Bus bus = Bus.builder().busId(1L).busNumber("ND-1001").operatorName("SLTB").build();
        Route route = Route.builder().routeId(1L).sourceCity("Colombo").destinationCity("Kandy").build();
        Trip trip = Trip.builder()
                .tripId(10L)
                .bus(bus)
                .route(route)
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(3))
                .baseFare(new BigDecimal("1200.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();

        when(tripRepository.findAllWithDetails()).thenReturn(List.of(trip));
        when(bookingRepository.findByTrip_TripId(10L)).thenReturn(Collections.emptyList());

        List<StaffTripSummary> trips = authService.getStaffTrips();

        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).getBusNumber()).isEqualTo("ND-1001");
        assertThat(trips.get(0).getBookedPassengerCount()).isEqualTo(0);
    }
}
