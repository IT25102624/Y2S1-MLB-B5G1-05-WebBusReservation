package com.system.busreservation.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyDto {
    private Long tripId;
    private String busNumber;
    private String routeName;
    private String departureTime;
    private int totalSeats;
    private int bookedSeats;
    private double occupancyPercentage;
}