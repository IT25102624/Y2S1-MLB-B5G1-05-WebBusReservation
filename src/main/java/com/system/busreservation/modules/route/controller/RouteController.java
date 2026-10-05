package com.system.busreservation.modules.route.controller;

import com.system.busreservation.model.Route;
import com.system.busreservation.modules.route.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routes")
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    // ============================================================
    // CREATE
    // POST /routes
    // ============================================================

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {

        Route savedRoute = routeService.createRoute(route);

        return new ResponseEntity<>(
                savedRoute,
                HttpStatus.CREATED
        );
    }


    // ============================================================
    // READ ALL
    // GET /routes
    // ============================================================

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {

        List<Route> routes = routeService.getAllRoutes();

        return ResponseEntity.ok(routes);
    }


    // ============================================================
    // READ ONE
    // GET /routes/{id}
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteById(
            @PathVariable Long id) {

        Route route = routeService.getRouteById(id);

        return ResponseEntity.ok(route);
    }


    // ============================================================
    // UPDATE
    // PUT /routes/{id}
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(
            @PathVariable Long id,
            @RequestBody Route route) {

        Route updatedRoute =
                routeService.updateRoute(id, route);

        return ResponseEntity.ok(updatedRoute);
    }


    // ============================================================
    // DELETE
    // DELETE /routes/{id}
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoute(
            @PathVariable Long id) {

        routeService.deleteRoute(id);

        return ResponseEntity.ok(
                "Route deleted successfully"
        );
    }
}
