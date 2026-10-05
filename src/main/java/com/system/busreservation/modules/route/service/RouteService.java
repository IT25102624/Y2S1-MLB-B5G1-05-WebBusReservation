package com.system.busreservation.modules.route.service;

import com.system.busreservation.model.Route;
import com.system.busreservation.modules.search.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    // CREATE
    public Route createRoute(Route route) {
        return routeRepository.save(route);
    }

    // READ - All routes
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    // READ - One route
    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Route not found with ID: " + id)
                );
    }

    // UPDATE
    public Route updateRoute(Long id, Route routeDetails) {

        Route existingRoute = routeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Route not found with ID: " + id)
                );

        existingRoute.setSourceCity(routeDetails.getSourceCity());
        existingRoute.setDestinationCity(routeDetails.getDestinationCity());

        // Update estimated duration
        if (routeDetails.getEstimatedDurationHours() != null) {
            existingRoute.setEstimatedDurationHours(routeDetails.getEstimatedDurationHours());
        }

        return routeRepository.save(existingRoute);
    }

    // DELETE
    public void deleteRoute(Long id) {

        if (!routeRepository.existsById(id)) {
            throw new RuntimeException(
                    "Route not found with ID: " + id
            );
        }

        routeRepository.deleteById(id);
    }
}
