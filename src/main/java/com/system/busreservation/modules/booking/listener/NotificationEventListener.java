package com.system.busreservation.modules.booking.listener;

import com.system.busreservation.modules.booking.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer (Event Listener) that creates an in-app notification
 * for the passenger upon booking confirmation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final JdbcTemplate jdbc;

    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[Observer:Notification] Processing booking confirmation event for booking #{}", event.getBookingId());

        if (event.getUserId() == null) {
            log.debug("[Observer:Notification] Anonymous booking, skipping user-specific in-app notification.");
            return;
        }

        try {
            jdbc.update("""
                    INSERT INTO user_notifications (recipient_user_id, booking_id, notification_type, subject, message, is_read, created_at)
                    VALUES (?, ?, ?, ?, ?, 0, SYSUTCDATETIME())
                    """,
                    event.getUserId(),
                    event.getBookingId(),
                    "PAYMENT_SUCCESS",
                    "Payment successful",
                    "Payment received for booking #" + event.getBookingId() + ". Your QR ticket is ready to download."
            );
            log.info("[Observer:Notification] Notification successfully persisted for user ID: {}", event.getUserId());
        } catch (Exception ex) {
            log.warn("[Observer:Notification] Failed to record in-app notification: {}", ex.getMessage());
        }
    }
}
