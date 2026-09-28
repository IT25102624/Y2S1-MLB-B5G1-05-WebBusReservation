package com.system.busreservation.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lists of available source and destination cities for search dropdowns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityPairResponse {

    private List<String> sourceCities;
    private List<String> destinationCities;
}
