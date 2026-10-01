package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdateRequest {

    private String sourceCity;

    private String destinationCity;
}