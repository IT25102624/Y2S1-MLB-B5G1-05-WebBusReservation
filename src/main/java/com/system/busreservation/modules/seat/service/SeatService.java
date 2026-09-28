package com.system.busreservation.modules.seat.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Seat;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.enums.DeckType;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import com.system.busreservation.modules.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional
    public Map<String, Object> getSeatMap(Long tripId) {
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        List<Seat> seats = seatRepository.findByBus_BusId(trip.getBus().getBusId());
        if (seats.isEmpty()) {
            for (int number = 1; number <= trip.getBus().getTotalSeats(); number++) {
                seats.add(Seat.builder()
                        .bus(trip.getBus())
                        .seatNumber(String.valueOf(number))
                        .deckType(DeckType.LOWER)
                        .isWindowSeat(number % 4 == 1 || number % 4 == 0)
                        .build());
            }
            seats = seatRepository.saveAll(seats);
        }
        seats.sort(Comparator.comparingInt(seat -> Integer.parseInt(seat.getSeatNumber())));
        Set<Long> booked = Set.copyOf(bookingSeatRepository.findActiveBookedSeatIds(tripId));
        List<Map<String, Object>> seatData = seats.stream().map(seat -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("seatId", seat.getSeatId());
            item.put("seatNumber", seat.getSeatNumber());
            item.put("windowSeat", seat.getIsWindowSeat());
            item.put("booked", booked.contains(seat.getSeatId()));
            return item;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tripId", trip.getTripId());
        result.put("busId", trip.getBus().getBusId());
        result.put("busNumber", trip.getBus().getBusNumber());
        result.put("operatorName", trip.getBus().getOperatorName());
        result.put("sourceCity", trip.getRoute().getSourceCity());
        result.put("destinationCity", trip.getRoute().getDestinationCity());
        result.put("departureTime", trip.getDepartureTime());
        result.put("arrivalTime", trip.getArrivalTime());
        result.put("baseFare", trip.getBaseFare());
        result.put("totalSeats", trip.getBus().getTotalSeats());
        result.put("availableSeats", trip.getBus().getTotalSeats() - booked.size());
        result.put("seats", seatData);
        return result;
    }
}
