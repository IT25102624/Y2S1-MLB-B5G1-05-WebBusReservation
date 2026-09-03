package com.system.busreservation.modules.search.service;

import com.system.busreservation.modules.search.dto.CityPairResponse;
import com.system.busreservation.modules.search.dto.RouteResponse;
import com.system.busreservation.modules.search.dto.TripSearchRequest;
import com.system.busreservation.modules.search.dto.TripSearchResponse;

import java.util.List;

/**
 * Service contract for route discovery and trip schedule searches.
 */
public interface SearchService {

    /**
     * Search available scheduled trips matching user query criteria.
     *
     * @param request Search criteria (source, destination, travel date, filters)
     * @return List of matching trips with live seat availability
     */
    List<TripSearchResponse> searchTrips(TripSearchRequest request);

    /**
     * Retrieve trip details and real-time seat availability by Trip ID.
     *
     * @param tripId Primary key of the trip
     * @return Detailed trip information
     */
    TripSearchResponse getTripDetails(Long tripId);

    /**
     * Get all registered travel routes across the network.
     *
     * @return List of all routes
     */
    List<RouteResponse> getAllRoutes();

    /**
     * Retrieve unique list of origin and destination cities for UI auto-complete.
     *
     * @return City lists for search inputs
     */
    CityPairResponse getAvailableCities();

    /**
     * Retrieve upcoming departures for a given Route ID.
     *
     * @param routeId Primary key of the route
     * @return List of upcoming scheduled trips
     */
    List<TripSearchResponse> getUpcomingTripsByRoute(Long routeId);
}
