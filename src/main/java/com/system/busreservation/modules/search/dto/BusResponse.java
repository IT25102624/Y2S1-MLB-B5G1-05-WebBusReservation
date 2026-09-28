package com.system.busreservation.modules.search.dto;

import com.system.busreservation.model.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO representing a fleet bus for trip scheduling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponse {

    private Long busId;
    private String busNumber;
    private BusType busType;
    private Integer totalSeats;
    private String operatorName;
}
