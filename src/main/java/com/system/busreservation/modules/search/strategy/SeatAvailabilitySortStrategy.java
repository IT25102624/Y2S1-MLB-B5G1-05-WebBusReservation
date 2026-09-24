package com.system.busreservation.modules.search.strategy;

import com.system.busreservation.modules.search.dto.TripSearchResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Concrete sorting strategy: Sorts scheduled bus trips by available/remaining seats.
 */
@Component
public class SeatAvailabilitySortStrategy implements TripSortStrategy {

    @Override
    public void sort(List<TripSearchResponse> trips, String sortKey) {
        if ("seatsasc".equalsIgnoreCase(sortKey)) {
            // Fewest available seats first
            trips.sort(Comparator.comparing(t -> t.getAvailableSeats() != null ? t.getAvailableSeats() : 0));
        } else {
            // Most available seats first (default)
            trips.sort(Comparator.comparing((TripSearchResponse t) -> t.getAvailableSeats() != null ? t.getAvailableSeats() : 0).reversed());
        }
    }

    @Override
    public boolean supports(String sortKey) {
        if (sortKey == null) {
            return false;
        }
        String key = sortKey.toLowerCase();
        return "seatsdesc".equals(key) || "seatsasc".equals(key)
                || "occupancydesc".equals(key) || "occupancyasc".equals(key);
    }
}
