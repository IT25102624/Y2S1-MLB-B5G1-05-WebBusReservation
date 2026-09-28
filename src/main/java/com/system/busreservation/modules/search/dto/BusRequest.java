package com.system.busreservation.modules.search.dto;

import com.system.busreservation.model.enums.BusType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a fleet bus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusRequest {

    @NotBlank(message = "Bus plate/registration number is required")
    private String busNumber;

    @NotNull(message = "Bus type is required")
    private BusType busType;

    @NotNull(message = "Total seating capacity is required")
    @Min(value = 10, message = "Total seating capacity must be at least 10 seats")
    @Max(value = 120, message = "Total seating capacity cannot exceed 120 seats")
    private Integer totalSeats;

    @NotBlank(message = "Operator or fleet company name is required")
    private String operatorName;
}
