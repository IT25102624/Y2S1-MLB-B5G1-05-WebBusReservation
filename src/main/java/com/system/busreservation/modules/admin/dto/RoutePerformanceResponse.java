package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePerformanceResponse {
    private Long routeId;
    private String sourceCity;
    private String destinationCity;
    private long totalTrips;
    private long totalBookings;
    private BigDecimal totalRevenue;
    private double averageOccupancyPercent;
}
