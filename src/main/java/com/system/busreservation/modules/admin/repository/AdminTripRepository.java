package com.system.busreservation.modules.admin.repository;

import com.system.busreservation.model.Trip;
import com.system.busreservation.model.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminTripRepository extends JpaRepository<Trip, Long> {

    @Query("""
           SELECT COUNT(t)
           FROM Trip t
           WHERE t.tripStatus = :status
           """)
    long countByStatus(
            @Param("status") TripStatus status
    );


    @Query("""
           SELECT COUNT(t)
           FROM Trip t
           WHERE t.bus.busId = :busId
             AND t.departureTime >= :from
           """)
    long countUpcomingTripsForBus(
            @Param("busId") Long busId,
            @Param("from") LocalDateTime from
    );


    @Query("""
           SELECT COALESCE(SUM(t.bus.totalSeats), 0)
           FROM Trip t
           WHERE t.departureTime >= :from
             AND t.departureTime < :to
             AND (:routeId IS NULL OR t.route.routeId = :routeId)
             AND (:busId IS NULL OR t.bus.busId = :busId)
           """)
    long calculateAvailableSeats(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("routeId") Long routeId,
            @Param("busId") Long busId
    );
}