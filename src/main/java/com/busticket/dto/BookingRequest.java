package com.busticket.dto;

import com.busticket.model.Passenger;

import java.util.List;

public class BookingRequest {

    private Long tripId;
    private String sessionId;
    private List<Passenger> passengers;
    private String primaryPassengerName;
    private String primaryPassengerEmail;
    private String primaryPassengerPhone;

    public BookingRequest() {}

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
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
