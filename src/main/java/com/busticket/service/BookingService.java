package com.busticket.service;

import com.busticket.dto.BookingRequest;
import com.busticket.dto.UpdateBookingRequest;
import com.busticket.dto.FareBreakdownDto;
import com.busticket.model.*;
import com.busticket.model.enums.BookingStatus;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.SeatLockRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final FareCalculatorService fareCalculatorService;
    private final SeatLockService seatLockService;

    public BookingService(BookingRepository bookingRepository,
                          TripRepository tripRepository,
                          SeatRepository seatRepository,
                          SeatLockRepository seatLockRepository,
                          FareCalculatorService fareCalculatorService,
                          SeatLockService seatLockService) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
        this.fareCalculatorService = fareCalculatorService;
        this.seatLockService = seatLockService;
    }

    @Transactional
    public Booking confirmBooking(BookingRequest request) {
        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new IllegalArgumentException("At least one passenger must be provided.");
        }

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + request.getTripId()));

        List<String> requestedSeatNumbers = request.getPassengers().stream()
                .map(Passenger::getSeatNumber)
                .collect(Collectors.toList());

        List<Seat> busSeats = seatRepository.findByBus(trip.getBus());
        Map<String, Seat> seatMapByNumber = busSeats.stream()
                .collect(Collectors.toMap(Seat::getSeatNumber, s -> s));

        List<Seat> selectedSeats = new ArrayList<>();
        for (String seatNum : requestedSeatNumbers) {
            Seat seat = seatMapByNumber.get(seatNum);
            if (seat == null) {
                throw new IllegalArgumentException("Seat " + seatNum + " does not exist for this bus.");
            }
            selectedSeats.add(seat);
        }

        // Verify all selected seats are held under valid, non-expired lock by this sessionId
        List<SeatLock> activeSessionLocks = seatLockRepository.findByTripIdAndSessionId(trip.getId(), request.getSessionId());
        Set<Long> lockedSeatIds = activeSessionLocks.stream()
                .filter(l -> !l.isExpired())
                .map(l -> l.getSeat().getId())
                .collect(Collectors.toSet());

        for (Seat seat : selectedSeats) {
            if (!lockedSeatIds.contains(seat.getId())) {
                throw new IllegalStateException("Seat " + seat.getSeatNumber() + " lock has expired or is invalid. Please select your seats again.");
            }
        }

        // Verify seats are not already booked
        List<Booking> existingBookings = bookingRepository.findByTripId(trip.getId());
        for (Booking existing : existingBookings) {
            if (existing.getStatus() == BookingStatus.CONFIRMED) {
                for (String bookedSeatNum : existing.getSeatNumbers()) {
                    if (requestedSeatNumbers.contains(bookedSeatNum)) {
                        throw new IllegalStateException("Seat " + bookedSeatNum + " has already been booked.");
                    }
                }
            }
        }

        // Calculate fare
        FareBreakdownDto fareDto = fareCalculatorService.calculateFare(trip, selectedSeats);

        // Generate PNR
        String pnr = generateUniquePnr();

        Booking booking = new Booking(
                pnr,
                trip,
                request.getPassengers(),
                requestedSeatNumbers,
                fareDto.getSubTotal(),
                fareDto.getTaxAmount() + fareDto.getConvenienceFee(),
                fareDto.getGrandTotal(),
                LocalDateTime.now(),
                BookingStatus.CONFIRMED,
                request.getPrimaryPassengerName(),
                request.getPrimaryPassengerEmail(),
                request.getPrimaryPassengerPhone()
        );

        Booking savedBooking = bookingRepository.save(booking);

        // Release seat locks
        List<Long> seatIdsToRelease = selectedSeats.stream().map(Seat::getId).collect(Collectors.toList());
        seatLockService.releaseSeats(trip.getId(), seatIdsToRelease, request.getSessionId());

        return savedBooking;
    }

    public Optional<Booking> getBookingByPnr(String pnr) {
        return bookingRepository.findByPnr(pnr);
    }

    private String generateUniquePnr() {
        String pnr;
        do {
            pnr = "BUS" + String.format("%06d", new Random().nextInt(1000000));
        } while (bookingRepository.findByPnr(pnr).isPresent());
        return pnr;
    }
    @Transactional
    public void cancelBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with PNR: " + pnr));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateBooking(String pnr, UpdateBookingRequest request) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with PNR: " + pnr));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled booking.");
        }

        if (request.getPrimaryPassengerName() != null) booking.setPrimaryPassengerName(request.getPrimaryPassengerName());
        if (request.getPrimaryPassengerEmail() != null) booking.setPrimaryPassengerEmail(request.getPrimaryPassengerEmail());
        if (request.getPrimaryPassengerPhone() != null) booking.setPrimaryPassengerPhone(request.getPrimaryPassengerPhone());
        
        if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
            Map<String, Passenger> newPassengerMap = request.getPassengers().stream()
                .collect(Collectors.toMap(Passenger::getSeatNumber, p -> p));

            for (Passenger existingPassenger : booking.getPassengers()) {
                Passenger newInfo = newPassengerMap.get(existingPassenger.getSeatNumber());
                if (newInfo != null) {
                    existingPassenger.setFullName(newInfo.getFullName());
                    existingPassenger.setAge(newInfo.getAge());
                    existingPassenger.setGender(newInfo.getGender());
                }
            }
        }
        return bookingRepository.save(booking);
    }
}
