package com.system.busreservation.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private int bookedPassengerCount;
}
