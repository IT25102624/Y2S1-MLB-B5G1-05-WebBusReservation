package com.system.busreservation.modules.admin.service;

import com.system.busreservation.modules.admin.dto.FleetRequest;
import com.system.busreservation.modules.admin.dto.FleetResponse;

import java.util.List;

public interface FleetService {

    List<FleetResponse> getAllBuses();

    FleetResponse getBusById(Long busId);

    FleetResponse createBus(FleetRequest request);

    FleetResponse updateBus(Long busId, FleetRequest request);

    void deleteBus(Long busId);
}
