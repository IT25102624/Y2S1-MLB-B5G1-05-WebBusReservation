package com.system.busreservation.modules.search.service;

import com.system.busreservation.modules.search.dto.BusResponse;
import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteRequest;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripCreateRequest;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;
import com.system.busreservation.modules.search.dto.TripUpdateRequest;

import java.util.List;

/**
 * Service contract for route discovery, schedule searches, and operator CRUD operations.
 */
public interface SearchService {

    // --- Passenger Search & Discovery Operations ---

    /**
     * Search available scheduled trips matching passenger query criteria.
     */
    List<TripSearchResponse> searchTrips(TripSearchRequest request);

    /**
     * Retrieve trip details and real-time seat availability by Trip ID.
     */
    TripSearchResponse getTripDetails(Long tripId);

    /**
     * Get all registered travel routes across the network.
     */
    List<RouteResponse> getAllRoutes();

    /**
     * Retrieve unique list of origin and destination cities for UI auto-complete.
     */
    CityPairResponse getAvailableCities();

    /**
     * Retrieve upcoming departures for a given Route ID.
     */
    List<TripSearchResponse> getUpcomingTripsByRoute(Long routeId);

    // --- Operator / Management CRUD Operations ---

    /**
     * Create a new bus route.
     */
    RouteResponse createRoute(RouteRequest request);

    /**
     * Update an existing route's details.
     */
    RouteResponse updateRoute(Long routeId, RouteRequest request);

    /**
     * Delete a route by ID (rejects if active trips depend on it).
     */
    void deleteRoute(Long routeId);

    /**
     * Schedule a new trip for a route and bus.
     */
    TripSearchResponse createTrip(TripCreateRequest request);

    /**
     * Schedule a batch of recurring trips across multiple days.
     */
    List<TripSearchResponse> createRecurringTrips(TripCreateRequest request);

    /**
     * Update/reschedule an existing trip.
     */
    TripSearchResponse updateTrip(Long tripId, TripUpdateRequest request);

    /**
     * Delete/cancel a trip by ID.
     */
    void deleteTrip(Long tripId);

    /**
     * Retrieve all scheduled trips for the operator management dashboard.
     */
    List<TripSearchResponse> getAllTrips();

    /**
     * Retrieve all fleet buses available for scheduling.
     */
    List<BusResponse> getAllBuses();
}
