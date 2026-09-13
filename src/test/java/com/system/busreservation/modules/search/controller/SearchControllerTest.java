package com.system.busreservation.modules.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.busreservation.common.config.SecurityConfig;
import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import com.system.busreservation.modules.search.dto.BusResponse;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteRequest;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripCreateRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.dto.TripUpdateRequest;
import com.system.busreservation.modules.search.service.SearchService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    // =========================================================================
    // Passenger Search Tests
    // =========================================================================

    @Test
    @DisplayName("GET /search/trips - Should return 200 and search results")
    void searchTrips_Success() throws Exception {
        LocalDate travelDate = LocalDate.now().plusDays(2);
        TripSearchResponse sampleTrip = TripSearchResponse.builder()
                .tripId(1L)
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .busNumber("ND-1001")
                .busType(BusType.AC)
                .operatorName("Superline Express")
                .departureTime(travelDate.atTime(8, 0))
                .arrivalTime(travelDate.atTime(11, 30))
                .baseFare(new BigDecimal("1200.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .totalSeats(40)
                .bookedSeats(5)
                .availableSeats(35)
                .build();

        when(searchService.searchTrips(any())).thenReturn(List.of(sampleTrip));

        mockMvc.perform(get("/search/trips")
                        .param("sourceCity", "Colombo")
                        .param("destinationCity", "Kandy")
                        .param("travelDate", travelDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].tripId").value(1L))
                .andExpect(jsonPath("$.data[0].sourceCity").value("Colombo"))
                .andExpect(jsonPath("$.data[0].destinationCity").value("Kandy"))
                .andExpect(jsonPath("$.data[0].availableSeats").value(35));
    }

    @Test
    @DisplayName("GET /search/cities - Should return searchable city lists")
    void getAvailableCities_Success() throws Exception {
        CityPairResponse cities = CityPairResponse.builder()
                .sourceCities(List.of("Colombo", "Kandy"))
                .destinationCities(List.of("Galle", "Jaffna"))
                .build();

        when(searchService.getAvailableCities()).thenReturn(cities);

        mockMvc.perform(get("/search/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceCities[0]").value("Colombo"))
                .andExpect(jsonPath("$.data.destinationCities[0]").value("Galle"));
    }

    @Test
    @DisplayName("GET /search/routes - Should return all routes")
    void getAllRoutes_Success() throws Exception {
        RouteResponse route = RouteResponse.builder()
                .routeId(1L)
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .estimatedDurationHours(new BigDecimal("3.50"))
                .build();

        when(searchService.getAllRoutes()).thenReturn(List.of(route));

        mockMvc.perform(get("/search/routes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].routeId").value(1L))
                .andExpect(jsonPath("$.data[0].sourceCity").value("Colombo"));
    }

    // =========================================================================
    // Operator CRUD Tests (Routes)
    // =========================================================================

    @Test
    @DisplayName("POST /search/routes - Should create route and return 201")
    void createRoute_Success() throws Exception {
        RouteRequest request = RouteRequest.builder()
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.00"))
                .build();

        RouteResponse response = RouteResponse.builder()
                .routeId(15L)
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.00"))
                .build();

        when(searchService.createRoute(any(RouteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/search/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.routeId").value(15L))
                .andExpect(jsonPath("$.data.sourceCity").value("Galle"));
    }

    @Test
    @DisplayName("PUT /search/routes/{routeId} - Should update route and return 200")
    void updateRoute_Success() throws Exception {
        RouteRequest request = RouteRequest.builder()
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.25"))
                .build();

        RouteResponse response = RouteResponse.builder()
                .routeId(15L)
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.25"))
                .build();

        when(searchService.updateRoute(eq(15L), any(RouteRequest.class))).thenReturn(response);

        mockMvc.perform(put("/search/routes/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estimatedDurationHours").value(1.25));
    }

    @Test
    @DisplayName("DELETE /search/routes/{routeId} - Should delete route and return 200")
    void deleteRoute_Success() throws Exception {
        mockMvc.perform(delete("/search/routes/15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Route deleted successfully"));

        verify(searchService).deleteRoute(15L);
    }

    // =========================================================================
    // Operator CRUD Tests (Trips & Buses)
    // =========================================================================

    @Test
    @DisplayName("POST /search/trips - Should schedule trip and return 201")
    void createTrip_Success() throws Exception {
        LocalDateTime dep = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime arr = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);

        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(2L)
                .departureTime(dep)
                .arrivalTime(arr)
                .baseFare(new BigDecimal("1400.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();

        TripSearchResponse response = TripSearchResponse.builder()
                .tripId(88L)
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .baseFare(new BigDecimal("1400.00"))
                .availableSeats(40)
                .build();

        when(searchService.createTrip(any(TripCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/search/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(88L));
    }

    @Test
    @DisplayName("DELETE /search/trips/{tripId} - Should delete trip and return 200")
    void deleteTrip_Success() throws Exception {
        mockMvc.perform(delete("/search/trips/88")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trip cancelled/deleted successfully"));

        verify(searchService).deleteTrip(88L);
    }

    @Test
    @DisplayName("GET /search/buses - Should return list of buses")
    void getAllBuses_Success() throws Exception {
        BusResponse bus = BusResponse.builder()
                .busId(1L)
                .busNumber("ND-1001")
                .busType(BusType.AC)
                .totalSeats(40)
                .operatorName("Superline Express")
                .build();

        when(searchService.getAllBuses()).thenReturn(List.of(bus));

        mockMvc.perform(get("/search/buses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].busNumber").value("ND-1001"));
    }
}
