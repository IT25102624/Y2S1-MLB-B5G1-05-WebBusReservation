package com.system.busreservation.modules.admin.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.model.Route;
import com.system.busreservation.modules.admin.dto.RouteUpdateRequest;
import com.system.busreservation.modules.search.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/routes")
@RequiredArgsConstructor
public class RouteManagementController {

    private final RouteRepository routeRepository;

    // ============================================================
    // UPDATE ROUTE
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Route>> updateRoute(
            @PathVariable Long id,
            @RequestBody RouteUpdateRequest request) {

        // Validate request
        if (request.getSourceCity() == null ||
                request.getSourceCity().trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.success(
                            "Source city is required",
                            null
                    )
            );
        }

        if (request.getDestinationCity() == null ||
                request.getDestinationCity().trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.success(
                            "Destination city is required",
                            null
                    )
            );
        }

        String sourceCity =
                request.getSourceCity().trim();

        String destinationCity =
                request.getDestinationCity().trim();

        // Check route exists
        Route route = routeRepository.findById(id)
                .orElse(null);

        if (route == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.success(
                                    "Route not found with ID: " + id,
                                    null
                            )
                    );
        }

        // Check duplicate route
        boolean duplicate =
                routeRepository
                        .existsBySourceCityIgnoreCaseAndDestinationCityIgnoreCaseAndRouteIdNot(
                                sourceCity,
                                destinationCity,
                                id
                        );

        if (duplicate) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.success(
                            "This route already exists",
                            null
                    )
            );
        }

        // Update route
        route.setSourceCity(sourceCity);
        route.setDestinationCity(destinationCity);

        Route updatedRoute =
                routeRepository.save(route);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Route updated successfully",
                        updatedRoute
                )
        );
    }


    // ============================================================
    // DELETE ROUTE
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable Long id) {

        // Check route exists
        if (!routeRepository.existsById(id)) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.success(
                                    "Route not found with ID: " + id,
                                    null
                            )
                    );
        }

        try {

            routeRepository.deleteById(id);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Route deleted successfully",
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(
                    HttpStatus.CONFLICT
            ).body(
                    ApiResponse.success(
                            "Cannot delete this route because it is being used by other records.",
                            null
                    )
            );
        }
    }
}