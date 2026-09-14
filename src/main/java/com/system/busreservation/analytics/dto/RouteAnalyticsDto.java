package com.system.busreservation.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteAnalyticsDto {
    private Long routeId;
    private String routeName;
    private String origin;
    private String destination;
    private long totalTrips;
    private long totalBookings;
    private BigDecimal totalRevenue;
    private double occupancyRate;
}