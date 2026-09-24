package com.system.busreservation.modules.search.strategy;

import com.system.busreservation.modules.search.dto.TripSearchResponse;

import java.util.List;

/**
 * GoF Strategy Pattern interface for sorting bus trip search results.
 * Allows sorting algorithms to vary independently from clients that use them.
 */
public interface TripSortStrategy {

    /**
     * Sort the provided list of trip responses in-place according to strategy logic.
     *
     * @param trips mutable list of trip search responses
     * @param sortKey the specific sort direction requested (e.g. fareAsc, fareDesc)
     */
    void sort(List<TripSearchResponse> trips, String sortKey);

    /**
     * Determines if this strategy supports the given sort key.
     *
     * @param sortKey query parameter string
     * @return true if this strategy handles the requested sort criteria
     */
    boolean supports(String sortKey);
}
