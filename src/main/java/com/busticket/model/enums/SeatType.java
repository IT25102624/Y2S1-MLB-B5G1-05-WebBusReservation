package com.busticket.model.enums;

public enum SeatType {
    SEATER_REGULAR("Regular Seater", 0.0),
    SEATER_WINDOW("Window Seater", 50.0),
    SLEEPER_SINGLE("Single Sleeper", 150.0),
    SLEEPER_DOUBLE("Double Sleeper", 250.0);

    private final String displayName;
    private final double priceSurcharge;

    SeatType(String displayName, double priceSurcharge) {
        this.displayName = displayName;
        this.priceSurcharge = priceSurcharge;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPriceSurcharge() {
        return priceSurcharge;
    }
}
