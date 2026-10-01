package com.system.busreservation.modules.admin.dto;

import com.system.busreservation.model.enums.BusType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FleetRequest {

    @NotBlank(message = "Bus number is required")
    private String busNumber;

    @NotBlank(message = "Operator name is required")
    private String operator;

    @NotNull(message = "Bus type is required")
    private BusType busType; // AC, NON_AC, LUXURY, SLEEPER

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;
}
