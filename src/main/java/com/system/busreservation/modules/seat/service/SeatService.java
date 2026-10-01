package com.system.busreservation.modules.seat.service;

import com.system.busreservation.model.BookingSeat;
import com.system.busreservation.model.Seat;
import com.system.busreservation.model.Trip;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.search.repository.TripRepository;
import com.system.busreservation.modules.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final BookingSeatRepository bookingSeatRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;

    public Map<String, Object> getSeatLayout(String busType) {
        boolean isLuxury = busType != null && busType.toUpperCase().contains("LUXURY");
        Map<String, Object> layout = new HashMap<>();

        if (isLuxury) {
            layout.put("busType", "AC_LUXURY");
            layout.put("label", "A/C Luxury Bus");
            layout.put("shortLabel", "A/C Luxury");
            layout.put("operator", "Luxury Coach");
            layout.put("totalSeats", 49);
            layout.put("rows", 13);
            layout.put("format", "2+2");
            List<List<Integer>> rows = new ArrayList<>();
            for (int r = 0; r < 12; r++) {
                int start = r * 4 + 1;
                rows.add(List.of(start, start + 1, start + 2, start + 3));
            }
            rows.add(List.of(49));
            layout.put("matrix", rows);
        } else {
            layout.put("busType", "NON_AC_NORMAL");
            layout.put("label", "Non A/C Normal Bus");
            layout.put("shortLabel", "Non A/C Normal");
            layout.put("operator", "SLTB / Ashok Leyland");
            layout.put("totalSeats", 54);
            layout.put("rows", 14);
            layout.put("format", "2+2");
            List<List<Integer>> rows = new ArrayList<>();
            for (int r = 0; r < 13; r++) {
                int start = r * 4 + 1;
                rows.add(List.of(start, start + 1, start + 2, start + 3));
            }
            rows.add(List.of(53, 54));
            layout.put("matrix", rows);
        }

        return layout;
    }

    public Map<String, Object> getTripSeats(Long tripId, String busType) {
        Map<String, Object> result = new HashMap<>(getSeatLayout(busType));
        result.put("tripId", tripId);

        Set<String> reserved = new HashSet<>();
        if (tripId != null && tripId > 0) {
            List<BookingSeat> booked = bookingSeatRepository.findByTrip_TripId(tripId);
            for (BookingSeat bs : booked) {
                if (bs.getSeat() != null && bs.getSeat().getSeatNumber() != null) {
                    reserved.add(bs.getSeat().getSeatNumber());
                }
            }
        }

        // If no DB seats booked yet for this trip, seed deterministic demo seats so the visual map looks realistic
        if (reserved.isEmpty()) {
            boolean isLuxury = busType != null && busType.toUpperCase().contains("LUXURY");
            List<Integer> demoReserved = isLuxury ? List.of(2, 7, 14, 21, 29, 36, 43) : List.of(3, 8, 11, 19, 27, 31, 38, 45, 52);
            for (int n : demoReserved) {
                reserved.add("S" + n);
            }
        }

        result.put("reservedSeats", reserved);
        int total = (int) result.get("totalSeats");
        result.put("availableSeatsCount", Math.max(0, total - reserved.size()));

        List<Map<String, Object>> seatList = new ArrayList<>();
        if (tripId != null && tripId > 0) {
            tripRepository.findById(tripId).ifPresent(trip -> {
                if (trip.getBus() != null) {
                    List<Seat> busSeats = seatRepository.findByBus_BusId(trip.getBus().getBusId());
                    for (Seat s : busSeats) {
                        Map<String, Object> sm = new HashMap<>();
                        sm.put("seatId", s.getSeatId());
                        sm.put("seatNumber", s.getSeatNumber());
                        sm.put("deckType", s.getDeckType() != null ? s.getDeckType().name() : "LOWER");
                        seatList.add(sm);
                    }
                }
            });
        }
        result.put("seats", seatList);
        return result;
    }
}
