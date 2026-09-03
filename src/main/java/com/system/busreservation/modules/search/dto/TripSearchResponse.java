package com.system.busreservation.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.system.busreservation.model.enums.BusType;
import com.system.busreservation.model.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detailed representation of a searchable bus trip with live seat availability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchResponse {

    private Long tripId;

    // Bus information
    private Long busId;
    private String busNumber;
    private BusType busType;
    private String operatorName;

    // Route information
    private Long routeId;
    private String sourceCity;
    private String destinationCity;
    private BigDecimal estimatedDurationHours;

    // Timing
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalTime;

    // Pricing & Availability
    private BigDecimal baseFare;
    private TripStatus tripStatus;
    private Integer totalSeats;
    private Integer bookedSeats;
    private Integer availableSeats;
}
