package com.system.busreservation.modules.search.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Bus;
import com.system.busreservation.model.Route;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.search.dto.BusRequest;
import com.system.busreservation.modules.search.dto.BusResponse;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteRequest;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripCreateRequest;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.dto.TripUpdateRequest;
import com.system.busreservation.modules.search.repository.BusRepository;
import com.system.busreservation.modules.search.repository.RouteRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import com.system.busreservation.modules.search.strategy.DepartureTimeSortStrategy;
import com.system.busreservation.modules.search.strategy.FareSortStrategy;
import com.system.busreservation.modules.search.strategy.SeatAvailabilitySortStrategy;
import com.system.busreservation.modules.search.strategy.TripSortStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private BusRepository busRepository;

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

        TripSortStrategyFactory strategyFactory = new TripSortStrategyFactory(List.of(
                new DepartureTimeSortStrategy(),
                new FareSortStrategy(),
                new SeatAvailabilitySortStrategy()
        ));

        searchService = new SearchServiceImpl(
                routeRepository,
                tripRepository,
                bookingSeatRepository,
                busRepository,
                strategyFactory
        );
    }

    // =========================================================================
    // Search / Read Tests
    // =========================================================================

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

        List<Object[]> mockSeatCounts = Collections.singletonList(new Object[]{10L, 5L});
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
        assertThat(response.getAvailableSeats()).isEqualTo(35);
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
    @DisplayName("Should return trip details by ID with accurate booked seats")
    void getTripDetails_Success() {
        when(tripRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testTrip));
        when(bookingSeatRepository.countActiveBookedSeatsByTripId(10L)).thenReturn(3);

        TripSearchResponse response = searchService.getTripDetails(10L);

        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getAvailableSeats()).isEqualTo(37);
        assertThat(response.getBusNumber()).isEqualTo("ND-1001");
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
    @DisplayName("Should sort search results by fare ascending using FareSortStrategy")
    void searchTrips_SortByFare_UsesStrategy() {
        Trip tripCheap = Trip.builder()
                .tripId(11L)
                .bus(testBus)
                .route(testRoute)
                .departureTime(LocalDate.now().plusDays(1).atTime(9, 0))
                .arrivalTime(LocalDate.now().plusDays(1).atTime(12, 30))
                .baseFare(new BigDecimal("900.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();

        Trip tripExpensive = Trip.builder()
                .tripId(12L)
                .bus(testBus)
                .route(testRoute)
                .departureTime(LocalDate.now().plusDays(1).atTime(10, 0))
                .arrivalTime(LocalDate.now().plusDays(1).atTime(13, 30))
                .baseFare(new BigDecimal("1500.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();

        TripSearchRequest request = TripSearchRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .travelDate(LocalDate.now().plusDays(1))
                .sortBy("fareAsc")
                .build();

        when(tripRepository.searchTrips(
                eq("Colombo"), eq("Kandy"), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), eq(null), eq(null), eq(TripStatus.SCHEDULED)
        )).thenReturn(List.of(tripExpensive, tripCheap));

        when(bookingSeatRepository.countActiveBookedSeatsByTripIds(any())).thenReturn(Collections.emptyList());

        List<TripSearchResponse> responses = searchService.searchTrips(request);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTripId()).isEqualTo(11L);
        assertThat(responses.get(0).getBaseFare()).isEqualByComparingTo("900.00");
        assertThat(responses.get(1).getTripId()).isEqualTo(12L);
        assertThat(responses.get(1).getBaseFare()).isEqualByComparingTo("1500.00");
    }

    // =========================================================================
    // Operator CRUD Tests (Routes)
    // =========================================================================

    @Test
    @DisplayName("createRoute - Should create route successfully")
    void createRoute_Success() {
        RouteRequest request = RouteRequest.builder()
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.00"))
                .build();

        Route savedRoute = Route.builder()
                .routeId(20L)
                .sourceCity("Galle")
                .destinationCity("Matara")
                .estimatedDurationHours(new BigDecimal("1.00"))
                .build();

        when(routeRepository.existsBySourceCityIgnoreCaseAndDestinationCityIgnoreCase("Galle", "Matara")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(savedRoute);

        RouteResponse response = searchService.createRoute(request);

        assertThat(response.getRouteId()).isEqualTo(20L);
        assertThat(response.getSourceCity()).isEqualTo("Galle");
        assertThat(response.getDestinationCity()).isEqualTo("Matara");
    }

    @Test
    @DisplayName("createRoute - Should throw exception when route already exists")
    void createRoute_Duplicate_ThrowsException() {
        RouteRequest request = RouteRequest.builder()
                .sourceCity("Colombo")
                .destinationCity("Kandy")
                .estimatedDurationHours(new BigDecimal("3.50"))
                .build();

        when(routeRepository.existsBySourceCityIgnoreCaseAndDestinationCityIgnoreCase("Colombo", "Kandy")).thenReturn(true);

        assertThatThrownBy(() -> searchService.createRoute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("deleteRoute - Should delete route when no active trips depend on it")
    void deleteRoute_Success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(tripRepository.countByRoute_RouteId(1L)).thenReturn(0L);

        searchService.deleteRoute(1L);

        verify(routeRepository).delete(testRoute);
    }

    @Test
    @DisplayName("deleteRoute - Should throw exception if trips depend on the route")
    void deleteRoute_WithActiveTrips_ThrowsException() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(tripRepository.countByRoute_RouteId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> searchService.deleteRoute(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scheduled trip(s) depend on it");
    }

    // =========================================================================
    // Operator CRUD Tests (Trips & Buses)
    // =========================================================================

    @Test
    @DisplayName("createTrip - Should schedule new trip successfully")
    void createTrip_Success() {
        LocalDateTime dep = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0);
        LocalDateTime arr = LocalDateTime.now().plusDays(2).withHour(11).withMinute(30);

        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(1L)
                .departureTime(dep)
                .arrivalTime(arr)
                .baseFare(new BigDecimal("1250.00"))
                .tripStatus(TripStatus.SCHEDULED)
                .build();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setTripId(55L);
            return t;
        });

        TripSearchResponse response = searchService.createTrip(request);

        assertThat(response.getTripId()).isEqualTo(55L);
        assertThat(response.getBaseFare()).isEqualTo(new BigDecimal("1250.00"));
        assertThat(response.getTotalSeats()).isEqualTo(40);
        assertThat(response.getAvailableSeats()).isEqualTo(40);
    }

    @Test
    @DisplayName("createTrip - Should reject when arrival time is before departure time")
    void createTrip_InvalidTimings_ThrowsException() {
        LocalDateTime dep = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
        LocalDateTime arr = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0); // Before departure

        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(1L)
                .departureTime(dep)
                .arrivalTime(arr)
                .baseFare(new BigDecimal("1250.00"))
                .build();

        assertThatThrownBy(() -> searchService.createTrip(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arrival time must be after departure time");
    }

    @Test
    @DisplayName("createTrip - Should throw IllegalArgumentException when departure time is in the past")
    void createTrip_PastDepartureTime_ThrowsException() {
        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(1L)
                .departureTime(LocalDateTime.now().minusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(1))
                .baseFare(new BigDecimal("1250.00"))
                .build();

        assertThatThrownBy(() -> searchService.createTrip(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Departure time cannot be in the past");
    }

    @Test
    @DisplayName("deleteTrip - Should cancel trip if active bookings exist")
    void deleteTrip_WithBookings_SetsStatusCancelled() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(testTrip));
        when(bookingSeatRepository.countActiveBookedSeatsByTripId(10L)).thenReturn(2);

        searchService.deleteTrip(10L);

        assertThat(testTrip.getTripStatus()).isEqualTo(TripStatus.CANCELLED);
        verify(tripRepository).save(testTrip);
    }

    @Test
    @DisplayName("getAllBuses - Should return fleet bus list")
    void getAllBuses_Success() {
        when(busRepository.findAll()).thenReturn(List.of(testBus));

        List<BusResponse> buses = searchService.getAllBuses();

        assertThat(buses).hasSize(1);
        assertThat(buses.get(0).getBusNumber()).isEqualTo("ND-1001");
        assertThat(buses.get(0).getTotalSeats()).isEqualTo(40);
    }

    @Test
    @DisplayName("createRecurringTrips - Should generate daily trips for specified repeat days")
    void createRecurringTrips_Daily_Success() {
        LocalDateTime dep = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime arr = LocalDateTime.now().plusDays(1).withHour(11).withMinute(30);

        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(1L)
                .departureTime(dep)
                .arrivalTime(arr)
                .baseFare(new BigDecimal("1250.00"))
                .isRecurring(true)
                .recurrenceType("DAILY")
                .repeatDays(7)
                .build();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(tripRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Trip> trips = invocation.getArgument(0);
            for (long i = 0; i < trips.size(); i++) {
                trips.get((int) i).setTripId(100L + i);
            }
            return trips;
        });

        List<TripSearchResponse> responses = searchService.createRecurringTrips(request);

        assertThat(responses).hasSize(7);
        assertThat(responses.get(0).getBaseFare()).isEqualByComparingTo("1250.00");
        assertThat(responses.get(0).getSourceCity()).isEqualTo("Colombo");
        verify(tripRepository).saveAll(any());
    }

    @Test
    @DisplayName("createTrip - When isRecurring is true, delegates to recurring trip scheduling")
    void createTrip_WithRecurringFlag_DelegatesToRecurring() {
        LocalDateTime dep = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime arr = LocalDateTime.now().plusDays(1).withHour(11).withMinute(30);

        TripCreateRequest request = TripCreateRequest.builder()
                .routeId(1L)
                .busId(1L)
                .departureTime(dep)
                .arrivalTime(arr)
                .baseFare(new BigDecimal("1250.00"))
                .isRecurring(true)
                .recurrenceType("DAILY")
                .repeatDays(3)
                .build();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(tripRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Trip> trips = invocation.getArgument(0);
            for (long i = 0; i < trips.size(); i++) {
                trips.get((int) i).setTripId(200L + i);
            }
            return trips;
        });

        TripSearchResponse response = searchService.createTrip(request);

        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(200L);
        verify(tripRepository).saveAll(any());
    }

    // =========================================================================
    // Operator Bus & Fleet CRUD Tests
    // =========================================================================

    @Test
    @DisplayName("createBus - Should register new bus and auto-generate seat layout")
    void createBus_Success() {
        BusRequest request = BusRequest.builder()
                .busNumber("WP ND-2002")
                .busType(BusType.LUXURY)
                .totalSeats(40)
                .operatorName("Southern Express")
                .build();

        when(busRepository.existsByBusNumberIgnoreCase("WP ND-2002")).thenReturn(false);
        when(busRepository.save(any(Bus.class))).thenAnswer(inv -> {
            Bus b = inv.getArgument(0);
            b.setBusId(50L);
            return b;
        });

        BusResponse response = searchService.createBus(request);

        assertThat(response).isNotNull();
        assertThat(response.getBusId()).isEqualTo(50L);
        assertThat(response.getBusNumber()).isEqualTo("WP ND-2002");
        assertThat(response.getBusType()).isEqualTo(BusType.LUXURY);
        assertThat(response.getTotalSeats()).isEqualTo(40);
        verify(busRepository).save(any(Bus.class));
    }

    @Test
    @DisplayName("createBus - Should throw exception if bus plate number already exists")
    void createBus_DuplicatePlate_ThrowsException() {
        BusRequest request = BusRequest.builder()
                .busNumber("ND-1001")
                .busType(BusType.AC)
                .totalSeats(40)
                .operatorName("Superline Express")
                .build();

        when(busRepository.existsByBusNumberIgnoreCase("ND-1001")).thenReturn(true);

        assertThatThrownBy(() -> searchService.createBus(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateBus - Should update bus details successfully")
    void updateBus_Success() {
        BusRequest request = BusRequest.builder()
                .busNumber("ND-1001-UPDATED")
                .busType(BusType.SLEEPER)
                .totalSeats(40)
                .operatorName("Superline Premier")
                .build();

        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(busRepository.existsByBusNumberIgnoreCaseAndBusIdNot("ND-1001-UPDATED", 1L)).thenReturn(false);
        when(busRepository.save(any(Bus.class))).thenAnswer(inv -> inv.getArgument(0));

        BusResponse response = searchService.updateBus(1L, request);

        assertThat(response.getBusNumber()).isEqualTo("ND-1001-UPDATED");
        assertThat(response.getBusType()).isEqualTo(BusType.SLEEPER);
        assertThat(response.getOperatorName()).isEqualTo("Superline Premier");
    }

    @Test
    @DisplayName("deleteBus - Should delete bus when no active trips depend on it")
    void deleteBus_Success() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(tripRepository.countByBus_BusId(1L)).thenReturn(0L);

        searchService.deleteBus(1L);

        verify(busRepository).delete(testBus);
    }

    @Test
    @DisplayName("deleteBus - Should throw IllegalStateException when active trips are assigned to the bus")
    void deleteBus_WithAssignedTrips_ThrowsException() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(testBus));
        when(tripRepository.countByBus_BusId(1L)).thenReturn(4L);

        assertThatThrownBy(() -> searchService.deleteBus(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scheduled trip(s) depend on it");
    }

    @Test
    @DisplayName("getAvailableBusTypes - Should return all defined bus types")
    void getAvailableBusTypes_Success() {
        List<String> types = searchService.getAvailableBusTypes();

        assertThat(types).contains("AC", "NON_AC", "LUXURY", "SLEEPER", "SEMI_LUXURY", "SUPER_LUXURY");
    }
}
