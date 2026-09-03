package com.system.busreservation.modules.search.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Route;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.enums.TripStatus;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.repository.RouteRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Override
    public List<TripSearchResponse> searchTrips(TripSearchRequest request) {
        log.info("Searching trips from [{}] to [{}] on [{}]",
                request.getSourceCity(), request.getDestinationCity(), request.getTravelDate());

        // 1. Business Validation
        validateSearchCriteria(request);

        // 2. Date Window Calculation
        LocalDate travelDate = request.getTravelDate();
        LocalDateTime startTime = travelDate.atStartOfDay();
        LocalDateTime endTime = travelDate.atTime(LocalTime.MAX);

        // If searching for today, ignore departures that already passed
        if (travelDate.isEqual(LocalDate.now())) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(startTime)) {
                startTime = now;
            }
        }

        // Apply Time-of-Day filtering if requested
        if (request.getTimeOfDay() != null && !request.getTimeOfDay().isBlank()) {
            startTime = adjustStartTimeForTimeOfDay(travelDate, startTime, request.getTimeOfDay());
            endTime = adjustEndTimeForTimeOfDay(travelDate, endTime, request.getTimeOfDay());
        }

        // 3. Query matching scheduled trips
        List<Trip> matchingTrips = tripRepository.searchTrips(
                request.getSourceCity().trim(),
                request.getDestinationCity().trim(),
                startTime,
                endTime,
                request.getBusType(),
                request.getMinFare(),
                request.getMaxFare(),
                TripStatus.SCHEDULED
        );

        if (matchingTrips.isEmpty()) {
            log.info("No matching trips found for the given search criteria");
            return Collections.emptyList();
        }

        // 4. Batch fetch booked seats count to prevent N+1 queries
        List<Long> tripIds = matchingTrips.stream().map(Trip::getTripId).toList();
        Map<Long, Integer> bookedSeatsMap = fetchBookedSeatsMap(tripIds);

        // 5. Transform to Response DTOs
        List<TripSearchResponse> responses = matchingTrips.stream()
                .map(trip -> mapToTripSearchResponse(trip, bookedSeatsMap.getOrDefault(trip.getTripId(), 0)))
                .collect(Collectors.toList());

        // 6. Apply Sorting
        applySorting(responses, request.getSortBy());

        return responses;
    }

    @Override
    public TripSearchResponse getTripDetails(Long tripId) {
        log.info("Fetching details for trip ID: {}", tripId);
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));

        int bookedSeats = bookingSeatRepository.countActiveBookedSeatsByTripId(tripId);
        return mapToTripSearchResponse(trip, bookedSeats);
    }

    @Override
    public List<RouteResponse> getAllRoutes() {
        log.info("Fetching all registered routes");
        return routeRepository.findAll().stream()
                .map(this::mapToRouteResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CityPairResponse getAvailableCities() {
        log.info("Fetching distinct origin and destination cities");
        List<String> sourceCities = routeRepository.findDistinctSourceCities();
        List<String> destinationCities = routeRepository.findDistinctDestinationCities();

        return CityPairResponse.builder()
                .sourceCities(sourceCities)
                .destinationCities(destinationCities)
                .build();
    }

    @Override
    public List<TripSearchResponse> getUpcomingTripsByRoute(Long routeId) {
        log.info("Fetching upcoming trips for route ID: {}", routeId);
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route", "routeId", routeId);
        }

        List<Trip> upcomingTrips = tripRepository.findUpcomingByRouteId(
                routeId,
                LocalDateTime.now(),
                TripStatus.SCHEDULED
        );

        if (upcomingTrips.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tripIds = upcomingTrips.stream().map(Trip::getTripId).toList();
        Map<Long, Integer> bookedSeatsMap = fetchBookedSeatsMap(tripIds);

        return upcomingTrips.stream()
                .map(trip -> mapToTripSearchResponse(trip, bookedSeatsMap.getOrDefault(trip.getTripId(), 0)))
                .collect(Collectors.toList());
    }

    // --- Helper & Mapping Methods ---

    private void validateSearchCriteria(TripSearchRequest request) {
        if (request.getSourceCity().trim().equalsIgnoreCase(request.getDestinationCity().trim())) {
            throw new IllegalArgumentException("Source city and destination city cannot be the same.");
        }
        if (request.getTravelDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Travel date cannot be in the past.");
        }
        if (request.getMinFare() != null && request.getMaxFare() != null
                && request.getMinFare().compareTo(request.getMaxFare()) > 0) {
            throw new IllegalArgumentException("Minimum fare cannot be greater than maximum fare.");
        }
    }

    private LocalDateTime adjustStartTimeForTimeOfDay(LocalDate travelDate, LocalDateTime defaultStart, String timeOfDay) {
        return switch (timeOfDay.toUpperCase()) {
            case "MORNING" -> travelDate.atTime(4, 0);
            case "AFTERNOON" -> travelDate.atTime(12, 0);
            case "EVENING" -> travelDate.atTime(17, 0);
            case "NIGHT" -> travelDate.atTime(21, 0);
            default -> defaultStart;
        };
    }

    private LocalDateTime adjustEndTimeForTimeOfDay(LocalDate travelDate, LocalDateTime defaultEnd, String timeOfDay) {
        return switch (timeOfDay.toUpperCase()) {
            case "MORNING" -> travelDate.atTime(11, 59, 59);
            case "AFTERNOON" -> travelDate.atTime(16, 59, 59);
            case "EVENING" -> travelDate.atTime(20, 59, 59);
            case "NIGHT" -> travelDate.atTime(23, 59, 59);
            default -> defaultEnd;
        };
    }

    private Map<Long, Integer> fetchBookedSeatsMap(List<Long> tripIds) {
        List<Object[]> results = bookingSeatRepository.countActiveBookedSeatsByTripIds(tripIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }

    private TripSearchResponse mapToTripSearchResponse(Trip trip, int bookedSeats) {
        int totalSeats = trip.getBus().getTotalSeats() != null ? trip.getBus().getTotalSeats() : 0;
        int availableSeats = Math.max(0, totalSeats - bookedSeats);

        return TripSearchResponse.builder()
                .tripId(trip.getTripId())
                .busId(trip.getBus().getBusId())
                .busNumber(trip.getBus().getBusNumber())
                .busType(trip.getBus().getBusType())
                .operatorName(trip.getBus().getOperatorName())
                .routeId(trip.getRoute().getRouteId())
                .sourceCity(trip.getRoute().getSourceCity())
                .destinationCity(trip.getRoute().getDestinationCity())
                .estimatedDurationHours(trip.getRoute().getEstimatedDurationHours())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .baseFare(trip.getBaseFare())
                .tripStatus(trip.getTripStatus())
                .totalSeats(totalSeats)
                .bookedSeats(bookedSeats)
                .availableSeats(availableSeats)
                .build();
    }

    private RouteResponse mapToRouteResponse(Route route) {
        return RouteResponse.builder()
                .routeId(route.getRouteId())
                .sourceCity(route.getSourceCity())
                .destinationCity(route.getDestinationCity())
                .estimatedDurationHours(route.getEstimatedDurationHours())
                .build();
    }

    private void applySorting(List<TripSearchResponse> responses, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return;
        }

        switch (sortBy.toLowerCase()) {
            case "faredesc" -> responses.sort(Comparator.comparing(TripSearchResponse::getBaseFare).reversed());
            case "fareasc" -> responses.sort(Comparator.comparing(TripSearchResponse::getBaseFare));
            case "departuredesc" -> responses.sort(Comparator.comparing(TripSearchResponse::getDepartureTime).reversed());
            case "departureasc" -> responses.sort(Comparator.comparing(TripSearchResponse::getDepartureTime));
            default -> responses.sort(Comparator.comparing(TripSearchResponse::getDepartureTime));
        }
    }
}
