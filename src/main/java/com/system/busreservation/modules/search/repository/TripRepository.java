package com.system.busreservation.modules.search.repository;

import com.system.busreservation.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByRoute_SourceCityIgnoreCaseAndRoute_DestinationCityIgnoreCaseAndDepartureTimeBetween(
            String sourceCity, String destinationCity, LocalDateTime start, LocalDateTime end);
}
