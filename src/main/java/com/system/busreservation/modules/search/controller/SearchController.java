package com.system.busreservation.modules.search.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.search.dto.BusRequest;
import com.system.busreservation.modules.search.dto.BusResponse;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteRequest;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripCreateRequest;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.dto.TripUpdateRequest;
import com.system.busreservation.modules.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Route and Bus Schedule Search & CRUD Management.
 * Assigned to Imanjith R. D. D. (IT25102624).
 */
@RestController
@RequestMapping({"/search", "/api/v1/search"})
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    // =========================================================================
    // Passenger Search Endpoints
    // =========================================================================

    /**
     * Search available scheduled trips matching user query criteria.
     * GET /api/v1/search/trips?sourceCity=Colombo&destinationCity=Kandy&travelDate=2026-09-14
     */
    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<List<TripSearchResponse>>> searchTrips(@Valid @ModelAttribute TripSearchRequest request) {
        log.info("REST request to search trips: {}", request);
        List<TripSearchResponse> results = searchService.searchTrips(request);
        return ResponseEntity.ok(ApiResponse.<List<TripSearchResponse>>builder()
                .success(true)
                .message(String.format("Found %d scheduled trips", results.size()))
                .data(results)
                .build());
    }

    /**
     * Retrieve trip details and real-time seat availability by Trip ID.
     * GET /api/v1/search/trips/{tripId}
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<TripSearchResponse>> getTripDetails(@PathVariable Long tripId) {
        log.info("REST request to get trip details for ID: {}", tripId);
        TripSearchResponse trip = searchService.getTripDetails(tripId);
        return ResponseEntity.ok(ApiResponse.<TripSearchResponse>builder()
                .success(true)
                .message("Trip details retrieved successfully")
                .data(trip)
                .build());
    }

    /**
     * Retrieve distinct origin and destination cities for search dropdowns.
     * GET /api/v1/search/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<CityPairResponse>> getAvailableCities() {
        log.info("REST request to fetch searchable cities");
        CityPairResponse cities = searchService.getAvailableCities();
        return ResponseEntity.ok(ApiResponse.<CityPairResponse>builder()
                .success(true)
                .message("Searchable cities retrieved successfully")
                .data(cities)
                .build());
    }

    /**
     * Retrieve all active travel routes.
     * GET /api/v1/search/routes
     */
    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllRoutes() {
        log.info("REST request to list all routes");
        List<RouteResponse> routes = searchService.getAllRoutes();
        return ResponseEntity.ok(ApiResponse.<List<RouteResponse>>builder()
                .success(true)
                .message(String.format("Retrieved %d active routes", routes.size()))
                .data(routes)
                .build());
    }

    /**
     * Retrieve upcoming scheduled trips for a specific route.
     * GET /api/v1/search/routes/{routeId}/trips
     */
    @GetMapping("/routes/{routeId}/trips")
    public ResponseEntity<ApiResponse<List<TripSearchResponse>>> getUpcomingTripsByRoute(@PathVariable Long routeId) {
        log.info("REST request to list upcoming trips for route ID: {}", routeId);
        List<TripSearchResponse> trips = searchService.getUpcomingTripsByRoute(routeId);
        return ResponseEntity.ok(ApiResponse.<List<TripSearchResponse>>builder()
                .success(true)
                .message(String.format("Retrieved %d upcoming trips for route ID %d", trips.size(), routeId))
                .data(trips)
                .build());
    }

    // =========================================================================
    // Operator / Management CRUD Endpoints
    // =========================================================================

    /**
     * Create a new travel route.
     * POST /api/v1/search/routes
     */
    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody RouteRequest request) {
        log.info("REST request to create route: {}", request);
        RouteResponse created = searchService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<RouteResponse>builder()
                .success(true)
                .message("Route created successfully")
                .data(created)
                .build());
    }

    /**
     * Update an existing route by ID.
     * PUT /api/v1/search/routes/{routeId}
     */
    @PutMapping("/routes/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody RouteRequest request) {
        log.info("REST request to update route ID {}: {}", routeId, request);
        RouteResponse updated = searchService.updateRoute(routeId, request);
        return ResponseEntity.ok(ApiResponse.<RouteResponse>builder()
                .success(true)
                .message("Route updated successfully")
                .data(updated)
                .build());
    }

    /**
     * Delete a route by ID.
     * DELETE /api/v1/search/routes/{routeId}
     */
    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long routeId) {
        log.info("REST request to delete route ID: {}", routeId);
        searchService.deleteRoute(routeId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Route deleted successfully")
                .build());
    }

    /**
     * Retrieve all scheduled trips for the management dashboard.
     * GET /api/v1/search/trips/all
     */
    @GetMapping("/trips/all")
    public ResponseEntity<ApiResponse<List<TripSearchResponse>>> getAllTrips() {
        log.info("REST request to get all trips for operator dashboard");
        List<TripSearchResponse> trips = searchService.getAllTrips();
        return ResponseEntity.ok(ApiResponse.<List<TripSearchResponse>>builder()
                .success(true)
                .message(String.format("Retrieved %d scheduled trips", trips.size()))
                .data(trips)
                .build());
    }

    /**
     * Schedule a new bus trip.
     * POST /api/v1/search/trips
     */
    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripSearchResponse>> createTrip(@Valid @RequestBody TripCreateRequest request) {
        log.info("REST request to schedule trip: {}", request);
        TripSearchResponse created = searchService.createTrip(request);
        String message = Boolean.TRUE.equals(request.getIsRecurring())
                ? "Recurring trips scheduled successfully"
                : "Trip scheduled successfully";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<TripSearchResponse>builder()
                .success(true)
                .message(message)
                .data(created)
                .build());
    }

    /**
     * Schedule a batch of recurring trips across multiple days.
     * POST /api/v1/search/trips/recurring
     */
    @PostMapping("/trips/recurring")
    public ResponseEntity<ApiResponse<List<TripSearchResponse>>> createRecurringTrips(@Valid @RequestBody TripCreateRequest request) {
        log.info("REST request to schedule recurring trips: {}", request);
        request.setIsRecurring(true);
        List<TripSearchResponse> createdTrips = searchService.createRecurringTrips(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<List<TripSearchResponse>>builder()
                .success(true)
                .message(String.format("Successfully scheduled %d recurring trips", createdTrips.size()))
                .data(createdTrips)
                .build());
    }

    /**
     * Update/reschedule an existing trip.
     * PUT /api/v1/search/trips/{tripId}
     */
    @PutMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<TripSearchResponse>> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripUpdateRequest request) {
        log.info("REST request to update trip ID {}: {}", tripId, request);
        TripSearchResponse updated = searchService.updateTrip(tripId, request);
        return ResponseEntity.ok(ApiResponse.<TripSearchResponse>builder()
                .success(true)
                .message("Trip updated successfully")
                .data(updated)
                .build());
    }

    /**
     * Cancel or delete a scheduled trip.
     * DELETE /api/v1/search/trips/{tripId}
     */
    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long tripId) {
        log.info("REST request to delete/cancel trip ID: {}", tripId);
        searchService.deleteTrip(tripId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Trip cancelled/deleted successfully")
                .build());
    }

    /**
     * List all fleet buses for scheduling.
     * GET /api/v1/search/buses
     */
    @GetMapping("/buses")
    public ResponseEntity<ApiResponse<List<BusResponse>>> getAllBuses() {
        log.info("REST request to list fleet buses");
        List<BusResponse> buses = searchService.getAllBuses();
        return ResponseEntity.ok(ApiResponse.<List<BusResponse>>builder()
                .success(true)
                .message(String.format("Retrieved %d fleet buses", buses.size()))
                .data(buses)
                .build());
    }

    /**
     * Register a new bus into the fleet.
     * POST /api/v1/search/buses
     */
    @PostMapping("/buses")
    public ResponseEntity<ApiResponse<BusResponse>> createBus(@Valid @RequestBody BusRequest request) {
        log.info("REST request to register new fleet bus: {}", request);
        BusResponse response = searchService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BusResponse>builder()
                        .success(true)
                        .message("Bus registered successfully")
                        .data(response)
                        .build());
    }

    /**
     * Update details of an existing fleet bus.
     * PUT /api/v1/search/buses/{busId}
     */
    @PutMapping("/buses/{busId}")
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(
            @PathVariable Long busId,
            @Valid @RequestBody BusRequest request) {
        log.info("REST request to update bus ID {}: {}", busId, request);
        BusResponse response = searchService.updateBus(busId, request);
        return ResponseEntity.ok(ApiResponse.<BusResponse>builder()
                .success(true)
                .message("Bus updated successfully")
                .data(response)
                .build());
    }

    /**
     * Remove a fleet bus by ID.
     * DELETE /api/v1/search/buses/{busId}
     */
    @DeleteMapping("/buses/{busId}")
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable Long busId) {
        log.info("REST request to delete bus ID: {}", busId);
        searchService.deleteBus(busId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Bus deleted successfully")
                .build());
    }

    /**
     * Retrieve all available bus types across the system.
     * GET /api/v1/search/bus-types
     */
    @GetMapping("/bus-types")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableBusTypes() {
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Retrieved available bus types")
                .data(searchService.getAvailableBusTypes())
                .build());
    }
}
