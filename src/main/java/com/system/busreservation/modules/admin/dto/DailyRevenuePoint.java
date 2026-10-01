package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenuePoint {

    private LocalDate date;

    private BigDecimal revenue;

    private long bookingCount;

    // Constructor required by JPQL query
    public DailyRevenuePoint(
            LocalDate date,
            BigDecimal revenue,
            Long bookingCount) {

        this.date = date;
        this.revenue = revenue;
        this.bookingCount = bookingCount == null ? 0 : bookingCount;
    }
}