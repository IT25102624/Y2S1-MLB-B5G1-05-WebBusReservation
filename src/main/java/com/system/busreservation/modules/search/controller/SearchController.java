package com.system.busreservation.modules.search.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Route and Bus Schedule Search.
 * Assigned to Imanjith R. D. D. (IT25102624).
 */
@RestController
@RequestMapping({"/search", "/api/v1/search"})
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    /**
     * Search available scheduled trips matching user query criteria.
     * GET /api/v1/search/trips?sourceCity=Colombo&destinationCity=Kandy&travelDate=2026-09-04
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
}
