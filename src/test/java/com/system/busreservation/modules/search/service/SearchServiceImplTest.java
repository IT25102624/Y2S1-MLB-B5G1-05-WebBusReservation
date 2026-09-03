package com.system.busreservation.modules.search.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Bus;
import com.system.busreservation.model.Route;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.repository.RouteRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Route testRoute;
    private Bus testBus;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testRoute = Route.builder()
                .routeId(1L)
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .estimatedDurationHours(new BigDecimal("3.50"))
                .build();

        testBus = Bus.builder()
                .busId(1L)
                .busNumber("ND-1001")
                .busType(BusType.AC)
                .totalSeats(40)
                .operatorName("Superline Express")
                .build();

        testTrip = Trip.builder()
                .tripId(10L)
                .bus(testBus)
                .route(testRoute)
                .departureTime(LocalDate.now().plusDays(1).atTime(8, 30))
                .arrivalTime(LocalDate.now().plusDays(1).atTime(12, 0))
                .baseFare(new BigDecimal("1200.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("Should successfully search trips and calculate available seats correctly")
    void searchTrips_Success() {
        TripSearchRequest request = TripSearchRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .travelDate(LocalDate.now().plusDays(1))
                .build();

        when(tripRepository.searchTrips(
                eq("Colombo"), eq("Kandy"), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), eq(null), eq(null), eq(TripStatus.SCHEDULED)
        )).thenReturn(List.of(testTrip));

        // 5 booked seats on trip 10
        List<Object[]> mockSeatCounts = java.util.Collections.singletonList(new Object[]{10L, 5L});
        when(bookingSeatRepository.countActiveBookedSeatsByTripIds(List.of(10L)))
                .thenReturn(mockSeatCounts);

        List<TripSearchResponse> responses = searchService.searchTrips(request);

        assertThat(responses).hasSize(1);
        TripSearchResponse response = responses.get(0);
        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getSourceCity()).isEqualTo("Colombo");
        assertThat(response.getDestinationCity()).isEqualTo("Kandy");
        assertThat(response.getTotalSeats()).isEqualTo(40);
        assertThat(response.getBookedSeats()).isEqualTo(5);
        assertThat(response.getAvailableSeats()).isEqualTo(35); // 40 - 5 = 35
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when source and destination are the same")
    void searchTrips_SameOriginAndDestination_ThrowsException() {
        TripSearchRequest request = TripSearchRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Colombo")
                .travelDate(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> searchService.searchTrips(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source city and destination city cannot be the same");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when travel date is in the past")
    void searchTrips_PastDate_ThrowsException() {
        TripSearchRequest request = TripSearchRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .travelDate(LocalDate.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> searchService.searchTrips(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Travel date cannot be in the past");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when minFare > maxFare")
    void searchTrips_MinFareGreaterThanMaxFare_ThrowsException() {
        TripSearchRequest request = TripSearchRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .travelDate(LocalDate.now().plusDays(1))
                .minFare(new BigDecimal("2000.00"))
                .maxFare(new BigDecimal("1000.00"))
                .build();

        assertThatThrownBy(() -> searchService.searchTrips(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum fare cannot be greater than maximum fare");
    }

    @Test
    @DisplayName("Should return trip details by ID with accurate booked seats")
    void getTripDetails_Success() {
        when(tripRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testTrip));
        when(bookingSeatRepository.countActiveBookedSeatsByTripId(10L)).thenReturn(3);

        TripSearchResponse response = searchService.getTripDetails(10L);

        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getAvailableSeats()).isEqualTo(37); // 40 - 3 = 37
        assertThat(response.getBusNumber()).isEqualTo("ND-1001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trip ID does not exist")
    void getTripDetails_NotFound_ThrowsException() {
        when(tripRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.getTripDetails(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return distinct origin and destination cities")
    void getAvailableCities_Success() {
        when(routeRepository.findDistinctSourceCities()).thenReturn(List.of("Colombo", "Galle", "Kandy"));
        when(routeRepository.findDistinctDestinationCities()).thenReturn(List.of("Badulla", "Jaffna", "Kandy"));

        CityPairResponse response = searchService.getAvailableCities();

        assertThat(response.getSourceCities()).containsExactly("Colombo", "Galle", "Kandy");
        assertThat(response.getDestinationCities()).containsExactly("Badulla", "Jaffna", "Kandy");
    }

    @Test
    @DisplayName("Should return all routes correctly")
    void getAllRoutes_Success() {
        when(routeRepository.findAll()).thenReturn(List.of(testRoute));

        List<RouteResponse> routes = searchService.getAllRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getSourceCity()).isEqualTo("Colombo");
        assertThat(routes.get(0).getDestinationCity()).isEqualTo("Kandy");
    }
}
