package com.system.busreservation.modules.search.strategy;

import com.system.busreservation.modules.search.dto.TripSearchResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Concrete sorting strategy: Sorts scheduled bus trips by base ticket fare.
 */
@Component
public class FareSortStrategy implements TripSortStrategy {

    @Override
    public void sort(List<TripSearchResponse> trips, String sortKey) {
        if ("faredesc".equalsIgnoreCase(sortKey)) {
            trips.sort(Comparator.comparing(TripSearchResponse::getBaseFare).reversed());
        } else {
            // Default to lowest fare first
            trips.sort(Comparator.comparing(TripSearchResponse::getBaseFare));
        }
    }

    @Override
    public boolean supports(String sortKey) {
        if (sortKey == null) {
            return false;
        }
        String key = sortKey.toLowerCase();
        return "fareasc".equals(key) || "faredesc".equals(key);
    }
}
