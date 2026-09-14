package com.busticket.dto;

import com.busticket.model.enums.DeckType;
import com.busticket.model.enums.SeatStatus;
import com.busticket.model.enums.SeatType;

public class SeatMapResponseDto {

    private Long id;
    private String seatNumber;
    private DeckType deckType;
    private int rowNum;
    private int colNum;
    private SeatType seatType;
    private String seatTypeDisplayName;
    private double calculatedPrice;
    private SeatStatus status; // AVAILABLE, LOCKED_BY_ME, LOCKED_BY_OTHER, BOOKED
    private boolean isLockedByMe;

    public SeatMapResponseDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSeatTypeDisplayName() {
        return seatTypeDisplayName;
    }

    public void setSeatTypeDisplayName(String seatTypeDisplayName) {
        this.seatTypeDisplayName = seatTypeDisplayName;
    }

    public double getCalculatedPrice() {
        return calculatedPrice;
    }

    public void setCalculatedPrice(double calculatedPrice) {
        this.calculatedPrice = calculatedPrice;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public boolean isLockedByMe() {
        return isLockedByMe;
    }

    public void setLockedByMe(boolean lockedByMe) {
        isLockedByMe = lockedByMe;
    }
}
