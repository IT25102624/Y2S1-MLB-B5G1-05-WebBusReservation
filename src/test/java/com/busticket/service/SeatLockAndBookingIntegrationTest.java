package com.busticket.service;

import com.busticket.dto.BookingRequest;
import com.busticket.dto.LockSeatResponse;
import com.busticket.model.*;
import com.busticket.model.enums.BookingStatus;
import com.busticket.model.enums.DeckType;
import com.busticket.model.enums.SeatType;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SeatLockAndBookingIntegrationTest {

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Trip testTrip;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        Bus bus = new Bus("TEST-BUS-01", "Test Express", "AC Seater", 20, false);
        busRepository.save(bus);

        seat1 = seatRepository.save(new Seat(bus, "S1", DeckType.LOWER, 1, 1, SeatType.SEATER_WINDOW));
        seat2 = seatRepository.save(new Seat(bus, "S2", DeckType.LOWER, 1, 2, SeatType.SEATER_REGULAR));

        testTrip = tripRepository.save(new Trip(
                bus, "City A", "City B",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(4),
                500.00
        ));
    }

    @Test
    void testSeatLocking_SuccessAndConflict() {
        String sessionUser1 = "SESS-USER-1";
        String sessionUser2 = "SESS-USER-2";

        // 1. User 1 locks seat 1 -> Should succeed
        LockSeatResponse response1 = seatLockService.tryLockSeats(testTrip.getId(), List.of(seat1.getId()), sessionUser1);
        assertTrue(response1.isSuccess());
        assertEquals(1, response1.getLockedSeatIds().size());

        // 2. User 2 attempts to lock seat 1 -> Should fail with conflict
        LockSeatResponse response2 = seatLockService.tryLockSeats(testTrip.getId(), List.of(seat1.getId()), sessionUser2);
        assertFalse(response2.isSuccess());
        assertTrue(response2.getMessage().contains("currently selected/locked"));

        // 3. User 1 releases lock
        seatLockService.releaseSeats(testTrip.getId(), List.of(seat1.getId()), sessionUser1);

        // 4. User 2 locks seat 1 again -> Should now succeed
        LockSeatResponse response3 = seatLockService.tryLockSeats(testTrip.getId(), List.of(seat1.getId()), sessionUser2);
        assertTrue(response3.isSuccess());
    }

    @Test
    void testConfirmBooking_FullFlow() {
        String sessionId = "SESS-BOOKING-TEST";

        // Lock seats
        seatLockService.tryLockSeats(testTrip.getId(), List.of(seat1.getId(), seat2.getId()), sessionId);

        // Prepare Booking request
        BookingRequest request = new BookingRequest();
        request.setTripId(testTrip.getId());
        request.setSessionId(sessionId);
        request.setPrimaryPassengerName("Alice Smith");
        request.setPrimaryPassengerEmail("alice@example.com");
        request.setPrimaryPassengerPhone("+1234567890");

        Passenger p1 = new Passenger("Alice Smith", 30, "Female", "S1");
        Passenger p2 = new Passenger("Bob Smith", 32, "Male", "S2");
        request.setPassengers(List.of(p1, p2));

        // Confirm Booking
        Booking booking = bookingService.confirmBooking(request);

        assertNotNull(booking);
        assertNotNull(booking.getPnr());
        assertTrue(booking.getPnr().startsWith("BUS"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(2, booking.getPassengers().size());

        // Verify saved in DB
        Optional<Booking> savedOpt = bookingRepository.findByPnr(booking.getPnr());
        assertTrue(savedOpt.isPresent());
        assertEquals("Alice Smith", savedOpt.get().getPrimaryPassengerName());
    }
}
