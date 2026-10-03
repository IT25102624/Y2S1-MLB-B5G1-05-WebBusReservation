package com.system.busreservation.modules.booking.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain Event dispatched when a passenger bus booking has been successfully confirmed.
 * Enables the Observer pattern so downstream listeners (notifications, email, analytics, auditing)
 * react independently to booking life-cycle changes.
 */
@Getter
@Builder
public class BookingConfirmedEvent {
    private final Long bookingId;
    private final Long userId;
    private final String passengerName;
    private final String passengerEmail;
    private final String passengerPhone;
    private final String ticketToken;
    private final String boardingPoint;
    private final String destinationPoint;
    private final BigDecimal totalAmount;
    private final String paymentMethod;
    private final String transactionReference;
    private final Instant confirmedAt;
}
