package com.system.busreservation.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalBookings;
    private BigDecimal totalRevenue;
    private long totalTrips;
    private long totalBuses;
    private double averageOccupancy;
    private long todayBookings;
    private BigDecimal todayRevenue;
}