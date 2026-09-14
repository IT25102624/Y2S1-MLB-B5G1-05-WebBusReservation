package com.busticket.model;

import com.busticket.model.enums.DeckType;
import com.busticket.model.enums.SeatType;
import jakarta.persistence.*;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(nullable = false)
    private String seatNumber; // e.g., "L1", "L2", "U1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeckType deckType; // LOWER, UPPER

    private int rowNum;
    private int colNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;

    public Seat() {}

    public Seat(Bus bus, String seatNumber, DeckType deckType, int rowNum, int colNum, SeatType seatType) {
        this.bus = bus;
        this.seatNumber = seatNumber;
        this.deckType = deckType;
        this.rowNum = rowNum;
        this.colNum = colNum;
        this.seatType = seatType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public DeckType getDeckType() {
        return deckType;
    }

    public void setDeckType(DeckType deckType) {
        this.deckType = deckType;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public int getColNum() {
        return colNum;
    }

    public void setColNum(int colNum) {
        this.colNum = colNum;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }
}
