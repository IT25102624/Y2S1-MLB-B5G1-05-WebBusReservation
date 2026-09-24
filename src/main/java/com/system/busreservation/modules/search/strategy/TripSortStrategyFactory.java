package com.system.busreservation.modules.search.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory pattern implementation for resolving the appropriate TripSortStrategy at runtime.
 * Adheres to the Open/Closed Principle (OCP) - new sorting strategies can be plugged in
 * without modifying client services.
 */
@Component
public class TripSortStrategyFactory {

    private final List<TripSortStrategy> strategies;
    private final DepartureTimeSortStrategy defaultStrategy;

    public TripSortStrategyFactory(List<TripSortStrategy> strategies) {
        this.strategies = strategies != null ? strategies : List.of();
        this.defaultStrategy = this.strategies.stream()
                .filter(s -> s instanceof DepartureTimeSortStrategy)
                .map(s -> (DepartureTimeSortStrategy) s)
                .findFirst()
                .orElseGet(DepartureTimeSortStrategy::new);
    }

    /**
     * Finds the matching strategy based on the query parameter sortKey.
     *
     * @param sortKey e.g. "fareAsc", "departureDesc", "seatsDesc"
     * @return the resolved TripSortStrategy, or the default departure time strategy
     */
    public TripSortStrategy getStrategy(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return defaultStrategy;
        }

        return strategies.stream()
                .filter(strategy -> !(strategy instanceof DepartureTimeSortStrategy) && strategy.supports(sortKey))
                .findFirst()
                .orElse(defaultStrategy);
    }
}
