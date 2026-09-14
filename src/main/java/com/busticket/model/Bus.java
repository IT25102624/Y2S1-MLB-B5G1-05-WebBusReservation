package com.busticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "buses")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String busNumber;

    @Column(nullable = false)
    private String operatorName;

    @Column(nullable = false)
    private String busType; // e.g., "A/C Sleeper (2+1)", "A/C Seater (2+2)"

    private int totalSeats;
    private boolean hasUpperDeck;

    public Bus() {}

    public Bus(String busNumber, String operatorName, String busType, int totalSeats, boolean hasUpperDeck) {
        this.busNumber = busNumber;
        this.operatorName = operatorName;
        this.busType = busType;
        this.totalSeats = totalSeats;
        this.hasUpperDeck = hasUpperDeck;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public boolean isHasUpperDeck() {
        return hasUpperDeck;
    }

    public void setHasUpperDeck(boolean hasUpperDeck) {
        this.hasUpperDeck = hasUpperDeck;
    }
}
