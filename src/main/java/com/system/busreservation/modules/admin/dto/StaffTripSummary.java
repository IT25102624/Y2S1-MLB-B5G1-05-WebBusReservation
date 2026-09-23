package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trip overview shown to conductors and drivers after they sign in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTripSummary {

    private Long tripId;
    private String busNumber;
    private String operatorName;
    private String sourceCity;
    private String destinationCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal baseFare;
    private String tripStatus;
    private long bookedPassengerCount;
}
