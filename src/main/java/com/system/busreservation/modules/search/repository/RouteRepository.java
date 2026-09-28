package com.system.busreservation.modules.search.repository;

import com.system.busreservation.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findBySourceCityIgnoreCaseAndDestinationCityIgnoreCase(String sourceCity, String destinationCity);

    Optional<Route> findFirstBySourceCityIgnoreCaseAndDestinationCityIgnoreCase(String sourceCity, String destinationCity);

    boolean existsBySourceCityIgnoreCaseAndDestinationCityIgnoreCase(String sourceCity, String destinationCity);

    boolean existsBySourceCityIgnoreCaseAndDestinationCityIgnoreCaseAndRouteIdNot(String sourceCity, String destinationCity, Long routeId);

    @Query("SELECT DISTINCT r.sourceCity FROM Route r ORDER BY r.sourceCity ASC")
    List<String> findDistinctSourceCities();

    @Query("SELECT DISTINCT r.destinationCity FROM Route r ORDER BY r.destinationCity ASC")
    List<String> findDistinctDestinationCities();

    @Query("SELECT DISTINCT r.destinationCity FROM Route r WHERE LOWER(r.sourceCity) = LOWER(:sourceCity) ORDER BY r.destinationCity ASC")
    List<String> findDestinationsBySourceCity(@Param("sourceCity") String sourceCity);
}
