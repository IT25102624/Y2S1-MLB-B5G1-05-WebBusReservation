package com.system.busreservation.modules.admin.dto;

import com.system.busreservation.model.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FleetResponse {
    private Long busId;
    private String busNumber;
    private String operator;
    private BusType busType;
    private int totalSeats;
    private long upcomingTripCount;
}
