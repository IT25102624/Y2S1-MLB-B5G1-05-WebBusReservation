package com.system.busreservation.modules.search.controller;

import com.system.busreservation.common.config.SecurityConfig;
import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

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

    @Test
    @DisplayName("GET /search/trips/{tripId} - Should return single trip details")
    void getTripDetails_Success() throws Exception {
        TripSearchResponse sampleTrip = TripSearchResponse.builder()
                .tripId(5L)
                .sourceCity("Colombo")
                .destinationCity("Galle")
                .busNumber("SP-4004")
                .busType(BusType.NON_AC)
                .totalSeats(50)
                .bookedSeats(0)
                .availableSeats(50)
                .baseFare(new BigDecimal("750.00"))
                .build();

        when(searchService.getTripDetails(5L)).thenReturn(sampleTrip);

        mockMvc.perform(get("/search/trips/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(5L))
                .andExpect(jsonPath("$.data.availableSeats").value(50));
    }
}
