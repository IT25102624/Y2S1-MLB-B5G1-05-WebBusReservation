package com.system.busreservation.modules.booking.listener;

import com.system.busreservation.modules.booking.event.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer (Event Listener) that records audit trails and operational metrics
 * whenever a booking is finalized.
 */
@Component
@Slf4j
public class BookingAuditEventListener {

    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[Observer:Audit] AUDIT-LOG: Booking #{} finalized | Passenger: {} | UserID: {} | Route: {} -> {} | Amount: LKR {} | Method: {} | Ref: {}",
                event.getBookingId(),
                event.getPassengerName(),
                event.getUserId(),
                event.getBoardingPoint(),
                event.getDestinationPoint(),
                event.getTotalAmount(),
                event.getPaymentMethod(),
                event.getTransactionReference()
        );
    }
}
