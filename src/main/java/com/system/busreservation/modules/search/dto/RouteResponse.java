package com.system.busreservation.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary DTO for active bus routes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private Long routeId;
    private String sourceCity;
    private String destinationCity;
    private BigDecimal estimatedDurationHours;
}
