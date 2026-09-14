package com.busticket.repository;

import com.busticket.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(String source, String destination);
}