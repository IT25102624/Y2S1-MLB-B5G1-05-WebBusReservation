package com.system.busreservation.modules.search.dto;

import com.system.busreservation.model.enums.BusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request criteria for querying available bus schedules and trips.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchRequest {

    @NotBlank(message = "Source city is required")
    private String sourceCity;

    @NotBlank(message = "Destination city is required")
    private String destinationCity;

    @NotNull(message = "Travel date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate travelDate;

    private BusType busType;

    private BigDecimal minFare;

    private BigDecimal maxFare;

    /**
     * Optional filter: MORNING (04:00-12:00), AFTERNOON (12:00-17:00),
     * EVENING (17:00-21:00), NIGHT (21:00-04:00)
     */
    private String timeOfDay;

    /**
     * Optional sorting: departureTimeAsc, departureTimeDesc, fareAsc, fareDesc
     */
    private String sortBy;
}
