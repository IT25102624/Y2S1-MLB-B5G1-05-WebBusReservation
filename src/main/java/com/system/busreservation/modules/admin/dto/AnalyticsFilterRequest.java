package com.system.busreservation.modules.admin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AnalyticsFilterRequest {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Long routeId;   // filter by a specific route
    private Long busId;     // filter by a specific bus / fleet vehicle
}
