package com.system.busreservation.modules.booking.listener;

import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import com.system.busreservation.modules.booking.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer (Event Listener) that emails a detailed e-ticket confirmation
 * to the passenger upon booking confirmation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailTicketEventListener {

    private final NotificationDispatchService notificationDispatchService;

    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        String email = event.getPassengerEmail();
        if (email == null || email.isBlank() || email.endsWith("@catchbus.local")) {
            log.debug("[Observer:Email] Skipping external email dispatch for internal or guest email: {}", email);
            return;
        }

        log.info("[Observer:Email] Dispatching ticket confirmation email to [{}] for booking #{}", email, event.getBookingId());

        String subject = "CatchBusLK - Booking Confirmation #" + event.getBookingId();
        String body = String.format("""
                Dear %s,

                Thank you for reserving your bus journey with CatchBusLK.
                Your booking #%d has been confirmed!

                Trip Details:
                --------------------------------------------------
                Boarding Point    : %s
                Destination       : %s
                Total Paid        : LKR %s
                Payment Method    : %s
                Transaction Ref   : %s
                Ticket QR Token   : %s
                --------------------------------------------------

                Please have your QR code ready on your mobile device when boarding.
                Have a pleasant journey!

                Warm regards,
                CatchBusLK Operations Team
                """,
                event.getPassengerName() != null ? event.getPassengerName() : "Valued Passenger",
                event.getBookingId(),
                event.getBoardingPoint(),
                event.getDestinationPoint(),
                event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0.00",
                event.getPaymentMethod(),
                event.getTransactionReference(),
                event.getTicketToken()
        );

        try {
            notificationDispatchService.dispatchOperationalSummary(email, subject, body);
            log.info("[Observer:Email] Confirmation email successfully dispatched to [{}]", email);
        } catch (Exception ex) {
            log.warn("[Observer:Email] Could not send confirmation email to [{}]: {}", email, ex.getMessage());
        }
    }
}
