package com.system.busreservation.modules.search.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating travel routes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {

    @NotBlank(message = "Source city is required")
    private String sourceCity;

    @NotBlank(message = "Destination city is required")
    private String destinationCity;

    @NotNull(message = "Estimated duration is required")
    @DecimalMin(value = "0.1", message = "Estimated duration must be at least 0.1 hours")
    private BigDecimal estimatedDurationHours;
}
