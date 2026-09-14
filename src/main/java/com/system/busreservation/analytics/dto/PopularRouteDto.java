package com.system.busreservation.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularRouteDto {
    private String routeName;
    private String origin;
    private String destination;
    private long bookingCount;
    private BigDecimal revenue;
}