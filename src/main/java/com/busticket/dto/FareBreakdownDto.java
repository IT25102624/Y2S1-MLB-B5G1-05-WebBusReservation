package com.busticket.dto;

import java.util.List;
import java.util.Map;

public class FareBreakdownDto {

    private double baseRouteFare;
    private double seatUpgradesTotal;
    private double subTotal;
    private double taxAmount; // 5% GST
    private double convenienceFee;
    private double grandTotal;
    private int totalSeats;
    private List<Map<String, Object>> seatDetails;

    public FareBreakdownDto() {}

    public double getBaseRouteFare() {
        return baseRouteFare;
    }

    public void setBaseRouteFare(double baseRouteFare) {
        this.baseRouteFare = baseRouteFare;
    }

    public double getSeatUpgradesTotal() {
        return seatUpgradesTotal;
    }

    public void setSeatUpgradesTotal(double seatUpgradesTotal) {
        this.seatUpgradesTotal = seatUpgradesTotal;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(double convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
        this.grandTotal = grandTotal;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public List<Map<String, Object>> getSeatDetails() {
        return seatDetails;
    }

    public void setSeatDetails(List<Map<String, Object>> seatDetails) {
        this.seatDetails = seatDetails;
    }
}
