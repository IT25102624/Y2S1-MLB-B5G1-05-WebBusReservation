package com.busticket.model;

import com.busticket.model.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pnr;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ElementCollection
    @CollectionTable(name = "booking_passengers", joinColumns = @JoinColumn(name = "booking_id"))
    private List<Passenger> passengers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_number")
    private List<String> seatNumbers = new ArrayList<>();

    @Column(nullable = false)
    private double baseFare;

    @Column(nullable = false)
    private double taxAmount;

    @Column(nullable = false)
    private double totalFare;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private String primaryPassengerName;
    private String primaryPassengerEmail;
    private String primaryPassengerPhone;

    public Booking() {}

    public Booking(String pnr, Trip trip, List<Passenger> passengers, List<String> seatNumbers,
                   double baseFare, double taxAmount, double totalFare, LocalDateTime bookingTime,
                   BookingStatus status, String primaryPassengerName, String primaryPassengerEmail, String primaryPassengerPhone) {
        this.pnr = pnr;
        this.trip = trip;
        this.passengers = passengers;
        this.seatNumbers = seatNumbers;
        this.baseFare = baseFare;
        this.taxAmount = taxAmount;
        this.totalFare = totalFare;
        this.bookingTime = bookingTime;
        this.status = status;
        this.primaryPassengerName = primaryPassengerName;
        this.primaryPassengerEmail = primaryPassengerEmail;
        this.primaryPassengerPhone = primaryPassengerPhone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(double totalFare) {
        this.totalFare = totalFare;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPrimaryPassengerName() {
        return primaryPassengerName;
    }

    public void setPrimaryPassengerName(String primaryPassengerName) {
        this.primaryPassengerName = primaryPassengerName;
    }

    public String getPrimaryPassengerEmail() {
        return primaryPassengerEmail;
    }

    public void setPrimaryPassengerEmail(String primaryPassengerEmail) {
        this.primaryPassengerEmail = primaryPassengerEmail;
    }

    public String getPrimaryPassengerPhone() {
        return primaryPassengerPhone;
    }

    public void setPrimaryPassengerPhone(String primaryPassengerPhone) {
        this.primaryPassengerPhone = primaryPassengerPhone;
    }
}
