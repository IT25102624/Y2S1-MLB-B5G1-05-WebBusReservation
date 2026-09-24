package com.system.busreservation.modules.search.repository;

import com.system.busreservation.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    boolean existsByBusNumberIgnoreCase(String busNumber);

    boolean existsByBusNumberIgnoreCaseAndBusIdNot(String busNumber, Long busId);

    Optional<Bus> findByBusNumberIgnoreCase(String busNumber);
}

