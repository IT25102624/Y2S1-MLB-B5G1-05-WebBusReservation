package com.busticket.dto;

import com.busticket.model.Passenger;
import java.util.List;

public class UpdateBookingRequest {
    private String primaryPassengerName;
    private String primaryPassengerEmail;
    private String primaryPassengerPhone;
    private List<Passenger> passengers;

    public UpdateBookingRequest() {}

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

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }
}
