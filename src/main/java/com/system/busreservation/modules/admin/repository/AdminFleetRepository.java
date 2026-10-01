package com.system.busreservation.modules.admin.repository;

import com.system.busreservation.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * NOTE: Bus is a shared entity (model/Bus.java). If another module (e.g. seat/search)
 * already declares a JpaRepository<Bus, Long>, that is fine — Spring Data creates one
 * bean per interface. Just avoid duplicating the SAME method signature across modules,
 * and flag any schema change to Bus.java with the team first.
 */
@Repository
public interface AdminFleetRepository extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusNumber(String busNumber);

    boolean existsByBusNumber(String busNumber);
}
