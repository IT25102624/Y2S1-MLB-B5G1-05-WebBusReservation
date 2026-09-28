package com.system.busreservation.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.busreservation.common.config.SecurityConfig;
import com.system.busreservation.model.enums.UserRole;
import com.system.busreservation.modules.auth.dto.AdminDashboardStats;
import com.system.busreservation.modules.auth.dto.AuthResponse;
import com.system.busreservation.modules.auth.dto.LoginRequest;
import com.system.busreservation.modules.auth.dto.RegisterRequest;
import com.system.busreservation.modules.auth.dto.StaffTripSummary;
import com.system.busreservation.modules.auth.dto.UserResponse;
import com.system.busreservation.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register - 201 Created")
    void register_Success() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("kasun@busreservation.lk")
                .phoneNumber("+94771234567")
                .password("securePassword123")
                .role(UserRole.PASSENGER)
                .build();

        AuthResponse resp = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.builder()
                        .userId(5L)
                        .fullName("Kasun Perera")
                        .email("kasun@busreservation.lk")
                        .phoneNumber("+94771234567")
                        .role(UserRole.PASSENGER)
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("kasun@busreservation.lk"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 200 OK")
    void login_Success() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("kasun@busreservation.lk")
                .password("securePassword123")
                .build();

        AuthResponse resp = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.builder()
                        .userId(5L)
                        .fullName("Kasun Perera")
                        .email("kasun@busreservation.lk")
                        .role(UserRole.PASSENGER)
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/stats - 200 OK")
    void getStats_Success() throws Exception {
        AdminDashboardStats stats = AdminDashboardStats.builder()
                .totalUsers(15L)
                .totalPassengers(10L)
                .totalConductors(3L)
                .totalDrivers(1L)
                .totalAdmins(1L)
                .build();

        when(authService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(15));
    }

    @Test
    @DisplayName("GET /api/v1/admin/staff/trips - 200 OK")
    void getStaffTrips_Success() throws Exception {
        StaffTripSummary trip = StaffTripSummary.builder()
                .tripId(1L)
                .busNumber("WP ND-1001")
                .operatorName("SLTB")
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(3))
                .baseFare(new BigDecimal("1200.00"))
                .tripStatus("SCHEDULED")
                .bookedPassengerCount(12)
                .build();

        when(authService.getStaffTrips()).thenReturn(List.of(trip));

        mockMvc.perform(get("/api/v1/admin/staff/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].busNumber").value("WP ND-1001"))
                .andExpect(jsonPath("$.data[0].bookedPassengerCount").value(12));
    }
}
