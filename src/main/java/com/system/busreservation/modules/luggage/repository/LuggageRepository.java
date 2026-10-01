package com.system.busreservation.modules.luggage.repository;

import com.system.busreservation.model.Luggage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LuggageRepository extends JpaRepository<Luggage, Long> {

    List<Luggage> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<Luggage> findByBooking_BookingId(Long bookingId);

    Optional<Luggage> findByTagNumber(String tagNumber);

    boolean existsByTagNumber(String tagNumber);
}
