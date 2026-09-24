package com.system.busreservation.modules.search.strategy;

import com.system.busreservation.modules.search.dto.TripSearchResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Concrete sorting strategy: Sorts scheduled bus trips by departure time.
 */
@Component
public class DepartureTimeSortStrategy implements TripSortStrategy {

    @Override
    public void sort(List<TripSearchResponse> trips, String sortKey) {
        if ("departuredesc".equalsIgnoreCase(sortKey)) {
            trips.sort(Comparator.comparing(TripSearchResponse::getDepartureTime).reversed());
        } else {
            // Default to earliest departure first
            trips.sort(Comparator.comparing(TripSearchResponse::getDepartureTime));
        }
    }

    @Override
    public boolean supports(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return true; // Default fallback strategy
        }
        String key = sortKey.toLowerCase();
        return "departureasc".equals(key) || "departuredesc".equals(key);
    }
}
