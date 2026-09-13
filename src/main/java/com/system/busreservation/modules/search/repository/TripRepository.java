package com.system.busreservation.modules.search.repository;

import com.system.busreservation.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRoute_SourceCityIgnoreCaseAndRoute_DestinationCityIgnoreCaseAndDepartureTimeBetween(
            String sourceCity, String destinationCity, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.route r
        JOIN FETCH t.bus b
        WHERE LOWER(r.sourceCity) = LOWER(:sourceCity)
          AND LOWER(r.destinationCity) = LOWER(:destinationCity)
          AND t.departureTime >= :startTime
          AND t.departureTime <= :endTime
          AND (:busType IS NULL OR b.busType = :busType)
          AND (:minFare IS NULL OR t.baseFare >= :minFare)
          AND (:maxFare IS NULL OR t.baseFare <= :maxFare)
          AND t.tripStatus = :tripStatus
        ORDER BY t.departureTime ASC
    """)
    List<Trip> searchTrips(
            @Param("sourceCity") String sourceCity,
            @Param("destinationCity") String destinationCity,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("busType") BusType busType,
            @Param("minFare") BigDecimal minFare,
            @Param("maxFare") BigDecimal maxFare,
            @Param("tripStatus") TripStatus tripStatus
    );

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.route r
        JOIN FETCH t.bus b
        WHERE t.tripId = :tripId
    """)
    Optional<Trip> findByIdWithDetails(@Param("tripId") Long tripId);

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.route r
        JOIN FETCH t.bus b
        WHERE r.routeId = :routeId
          AND t.departureTime >= :now
          AND t.tripStatus = :tripStatus
        ORDER BY t.departureTime ASC
    """)
    List<Trip> findUpcomingByRouteId(
            @Param("routeId") Long routeId,
            @Param("now") LocalDateTime now,
            @Param("tripStatus") TripStatus tripStatus
    );

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.route r
        JOIN FETCH t.bus b
        ORDER BY t.departureTime DESC
    """)
    List<Trip> findAllWithDetails();

    long countByRoute_RouteId(Long routeId);
}
