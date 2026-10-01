package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryResponse {

    private BigDecimal totalRevenue;

    private long totalBookings;

    private long totalSeatsSold;

    private BigDecimal occupancyRatePercent;

    private List<DailyRevenuePoint> dailyTrend = new ArrayList<>();
}