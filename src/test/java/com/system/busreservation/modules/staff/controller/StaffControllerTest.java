package com.system.busreservation.modules.staff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.busreservation.common.config.SecurityConfig;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.auth.service.AuthService;
import com.system.busreservation.modules.staff.service.StaffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StaffService staffService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("GET /api/v1/staff/crew-roster returns list of scheduled trips with crew assignments")
    void testGetCrewRoster() throws Exception {
        when(staffService.getTripsWithCrew()).thenReturn(List.of(
                Map.of("tripId", 1L, "sourceCity", "Colombo", "destinationCity", "Kandy", "busNumber", "ND-1001")
        ));

        mockMvc.perform(get("/api/v1/staff/crew-roster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].tripId").value(1))
                .andExpect(jsonPath("$.data[0].sourceCity").value("Colombo"));

        verify(staffService).getTripsWithCrew();
    }

    @Test
    @DisplayName("GET /api/v1/staff/crew-members returns registered drivers and conductors")
    void testGetCrewMembers() throws Exception {
        when(staffService.getCrewMembers()).thenReturn(List.of(
                Map.of("userId", 5L, "fullName", "Kamal Perera", "role", "DRIVER"),
                Map.of("userId", 6L, "fullName", "Suneth Bandara", "role", "CONDUCTOR")
        ));

        mockMvc.perform(get("/api/v1/staff/crew-members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fullName").value("Kamal Perera"))
                .andExpect(jsonPath("$.data[1].fullName").value("Suneth Bandara"));

        verify(staffService).getCrewMembers();
    }

    @Test
    @DisplayName("POST /api/v1/staff/crew-assign assigns driver and conductor to a single trip")
    void testAssignCrew() throws Exception {
        StaffController.AssignCrewRequest request = new StaffController.AssignCrewRequest(10L, 5L, 6L);

        mockMvc.perform(post("/api/v1/staff/crew-assign?adminUserId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(10))
                .andExpect(jsonPath("$.data.assigned").value(true));

        verify(staffService).assignCrew(eq(10L), eq(5L), eq(6L), eq(1L));
    }

    @Test
    @DisplayName("POST /api/v1/staff/crew-assign-recurring assigns crew across recurring schedules")
    void testAssignRecurringCrew() throws Exception {
        StaffController.AssignRecurringCrewRequest request = new StaffController.AssignRecurringCrewRequest(
                1L, 2L, "06:30", "2026-10-05", "2026-10-12", true, 5L, 6L
        );

        when(staffService.assignRecurringCrew(eq(1L), eq(2L), eq("06:30"), eq("2026-10-05"), eq("2026-10-12"),
                eq(true), eq(5L), eq(6L), eq(1L)))
                .thenReturn(Map.of("assignedCount", 7, "message", "Assigned crew to 7 recurring trip(s)."));

        mockMvc.perform(post("/api/v1/staff/crew-assign-recurring?adminUserId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedCount").value(7));

        verify(staffService).assignRecurringCrew(eq(1L), eq(2L), eq("06:30"), eq("2026-10-05"), eq("2026-10-12"),
                eq(true), eq(5L), eq(6L), eq(1L));
    }

    @Test
    @DisplayName("POST /api/v1/staff/crew-assign-batch assigns crew across a list of trip IDs")
    void testAssignBatchCrew() throws Exception {
        StaffController.AssignBatchCrewRequest request = new StaffController.AssignBatchCrewRequest(
                List.of(10L, 11L, 12L), 5L, 6L
        );

        when(staffService.assignBatchCrew(eq(List.of(10L, 11L, 12L)), eq(5L), eq(6L), eq(1L)))
                .thenReturn(Map.of("assignedCount", 3, "message", "Batch assigned crew to 3 trip(s)."));

        mockMvc.perform(post("/api/v1/staff/crew-assign-batch?adminUserId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedCount").value(3));

        verify(staffService).assignBatchCrew(eq(List.of(10L, 11L, 12L)), eq(5L), eq(6L), eq(1L));
    }
}
