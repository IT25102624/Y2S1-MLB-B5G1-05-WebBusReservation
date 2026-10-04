package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Bus;
import com.system.busreservation.modules.admin.dto.FleetRequest;
import com.system.busreservation.modules.admin.dto.FleetResponse;
import com.system.busreservation.modules.admin.repository.AdminFleetRepository;
import com.system.busreservation.modules.admin.repository.AdminTripRepository;
import com.system.busreservation.modules.admin.service.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FleetServiceImpl implements FleetService {

    private final AdminFleetRepository fleetRepository;
    private final AdminTripRepository tripRepository;

    @Override
    public List<FleetResponse> getAllBuses() {
        return fleetRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FleetResponse getBusById(Long busId) {
        Bus bus = findBusOrThrow(busId);
        return toResponse(bus);
    }

    @Override
    @Transactional
    public FleetResponse createBus(FleetRequest request) {

        if (fleetRepository.existsByBusNumber(request.getBusNumber())) {
            throw new IllegalArgumentException(
                    "A bus with number "
                            + request.getBusNumber()
                            + " already exists"
            );
        }

        Bus bus = new Bus();

        applyRequest(bus, request);

        Bus savedBus = fleetRepository.save(bus);

        return toResponse(savedBus);
    }

    @Override
    @Transactional
    public FleetResponse updateBus(
            Long busId,
            FleetRequest request) {

        Bus bus = findBusOrThrow(busId);

        applyRequest(bus, request);

        Bus savedBus = fleetRepository.save(bus);

        return toResponse(savedBus);
    }

    @Override
    @Transactional
    public void deleteBus(Long busId) {

        Bus bus = findBusOrThrow(busId);

        fleetRepository.delete(bus);
    }

    private Bus findBusOrThrow(Long busId) {

        return fleetRepository.findById(busId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Bus not found with id: " + busId
                        )
                );
    }

    private void applyRequest(
            Bus bus,
            FleetRequest request) {

        bus.setBusNumber(request.getBusNumber());
        bus.setBusType(request.getBusType());
        bus.setTotalSeats(request.getTotalSeats());
    }

    private FleetResponse toResponse(Bus bus) {

        long upcomingTrips =
                tripRepository.countUpcomingTripsForBus(
                        bus.getBusId(),
                        LocalDateTime.now()
                );

        return new FleetResponse(
                bus.getBusId(),
                bus.getBusNumber(),
                bus.getOperatorName(),
                bus.getBusType(),
                bus.getTotalSeats(),
                upcomingTrips
        );
    }
}