package com.busticket.service;

import com.busticket.dto.FareBreakdownDto;
import com.busticket.model.Bus;
import com.busticket.model.Seat;
import com.busticket.model.Trip;
import com.busticket.model.enums.DeckType;
import com.busticket.model.enums.SeatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FareCalculatorServiceTest {

    private FareCalculatorService fareCalculatorService;
    private Trip trip;
    private Bus bus;

    @BeforeEach
    void setUp() {
        fareCalculatorService = new FareCalculatorService();
        bus = new Bus("KA-01-EX-9999", "Test Operator", "AC Sleeper", 30, true);
        trip = new Trip(bus, "Bangalore", "Goa", LocalDateTime.now(), LocalDateTime.now().plusHours(10), 1000.00);
    }

    @Test
    void testCalculateFare_SingleRegularSeat() {
        Seat seat1 = new Seat(bus, "L1", DeckType.LOWER, 1, 1, SeatType.SEATER_REGULAR); // 0 surcharge

        FareBreakdownDto breakdown = fareCalculatorService.calculateFare(trip, List.of(seat1));

        assertEquals(1, breakdown.getTotalSeats());
        assertEquals(1000.00, breakdown.getBaseRouteFare());
        assertEquals(0.00, breakdown.getSeatUpgradesTotal());
        assertEquals(1000.00, breakdown.getSubTotal());
        assertEquals(50.00, breakdown.getTaxAmount()); // 5% of 1000 = 50
        assertEquals(25.00, breakdown.getConvenienceFee());
        assertEquals(1075.00, breakdown.getGrandTotal()); // 1000 + 50 + 25
    }

    @Test
    void testCalculateFare_MultipleSeatsWithUpgrades() {
        Seat seat1 = new Seat(bus, "L1", DeckType.LOWER, 1, 1, SeatType.SLEEPER_SINGLE); // +150
        Seat seat2 = new Seat(bus, "L2", DeckType.LOWER, 1, 2, SeatType.SLEEPER_DOUBLE); // +250

        FareBreakdownDto breakdown = fareCalculatorService.calculateFare(trip, List.of(seat1, seat2));

        assertEquals(2, breakdown.getTotalSeats());
        assertEquals(2000.00, breakdown.getBaseRouteFare());
        assertEquals(400.00, breakdown.getSeatUpgradesTotal()); // 150 + 250
        assertEquals(2400.00, breakdown.getSubTotal()); // 2000 + 400
        assertEquals(120.00, breakdown.getTaxAmount()); // 5% of 2400
        assertEquals(25.00, breakdown.getConvenienceFee());
        assertEquals(2545.00, breakdown.getGrandTotal()); // 2400 + 120 + 25
    }

    @Test
    void testCalculateFare_EmptySeatList() {
        FareBreakdownDto breakdown = fareCalculatorService.calculateFare(trip, List.of());
        assertEquals(0, breakdown.getTotalSeats());
        assertEquals(0.00, breakdown.getGrandTotal());
    }
}
