package com.busticket.service;

import com.busticket.dto.FareBreakdownDto;
import com.busticket.model.Seat;
import com.busticket.model.Trip;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FareCalculatorService {

    public static final double GST_RATE = 0.05; // 5% GST
    public static final double CONVENIENCE_FEE = 25.00; // Flat convenience fee per transaction

    public FareBreakdownDto calculateFare(Trip trip, List<Seat> selectedSeats) {
        FareBreakdownDto dto = new FareBreakdownDto();

        if (selectedSeats == null || selectedSeats.isEmpty()) {
            dto.setTotalSeats(0);
            dto.setBaseRouteFare(0.0);
            dto.setSeatUpgradesTotal(0.0);
            dto.setSubTotal(0.0);
            dto.setTaxAmount(0.0);
            dto.setConvenienceFee(0.0);
            dto.setGrandTotal(0.0);
            dto.setSeatDetails(new ArrayList<>());
            return dto;
        }

        double basePrice = trip.getBasePrice();
        double totalSeatUpgrades = 0.0;
        List<Map<String, Object>> seatDetails = new ArrayList<>();

        for (Seat seat : selectedSeats) {
            double surcharge = seat.getSeatType().getPriceSurcharge();
            double seatTotal = basePrice + surcharge;
            totalSeatUpgrades += surcharge;

            Map<String, Object> detail = new HashMap<>();
            detail.put("seatNumber", seat.getSeatNumber());
            detail.put("deck", seat.getDeckType().name());
            detail.put("seatType", seat.getSeatType().getDisplayName());
            detail.put("baseFare", basePrice);
            detail.put("upgradeSurcharge", surcharge);
            detail.put("totalPrice", seatTotal);

            seatDetails.add(detail);
        }

        double subTotal = (basePrice * selectedSeats.size()) + totalSeatUpgrades;
        double taxAmount = Math.round((subTotal * GST_RATE) * 100.0) / 100.0;
        double grandTotal = Math.round((subTotal + taxAmount + CONVENIENCE_FEE) * 100.0) / 100.0;

        dto.setTotalSeats(selectedSeats.size());
        dto.setBaseRouteFare(basePrice * selectedSeats.size());
        dto.setSeatUpgradesTotal(totalSeatUpgrades);
        dto.setSubTotal(subTotal);
        dto.setTaxAmount(taxAmount);
        dto.setConvenienceFee(CONVENIENCE_FEE);
        dto.setGrandTotal(grandTotal);
        dto.setSeatDetails(seatDetails);

        return dto;
    }
}
