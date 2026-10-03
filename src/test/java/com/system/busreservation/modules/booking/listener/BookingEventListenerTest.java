package com.system.busreservation.modules.booking.listener;

import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import com.system.busreservation.modules.booking.event.BookingConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Observer Pattern - Booking Event Listeners Tests")
class BookingEventListenerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    private NotificationEventListener notificationEventListener;
    private EmailTicketEventListener emailTicketEventListener;
    private BookingAuditEventListener auditEventListener;

    @BeforeEach
    void setUp() {
        notificationEventListener = new NotificationEventListener(jdbcTemplate);
        emailTicketEventListener = new EmailTicketEventListener(notificationDispatchService);
        auditEventListener = new BookingAuditEventListener();
    }

    private BookingConfirmedEvent createSampleEvent(String email) {
        return BookingConfirmedEvent.builder()
                .bookingId(101L)
                .userId(5L)
                .passengerName("Kamal Gunaratne")
                .passengerEmail(email)
                .passengerPhone("+94771234567")
                .ticketToken("UUID-TICKET-TEST-TOKEN")
                .boardingPoint("Colombo Fort")
                .destinationPoint("Kandy Clock Tower")
                .totalAmount(new BigDecimal("1850.00"))
                .paymentMethod("Credit / Debit Card")
                .transactionReference("PAY-REF-998877")
                .confirmedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("NotificationEventListener should persist in-app notification when booking confirmed")
    void shouldPersistInAppNotification() {
        BookingConfirmedEvent event = createSampleEvent("kamal@gmail.com");

        notificationEventListener.handleBookingConfirmed(event);

        verify(jdbcTemplate, times(1)).update(
                anyString(),
                eq(5L),
                eq(101L),
                eq("PAYMENT_SUCCESS"),
                eq("Payment successful"),
                contains("booking #101")
        );
    }

    @Test
    @DisplayName("EmailTicketEventListener should dispatch email summary for valid passenger email")
    void shouldDispatchEmailTicket() {
        BookingConfirmedEvent event = createSampleEvent("kamal@gmail.com");

        emailTicketEventListener.handleBookingConfirmed(event);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationDispatchService, times(1))
                .dispatchOperationalSummary(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals("kamal@gmail.com", emailCaptor.getValue());
        assertTrue(subjectCaptor.getValue().contains("#101"));
        assertTrue(bodyCaptor.getValue().contains("Colombo Fort"));
        assertTrue(bodyCaptor.getValue().contains("UUID-TICKET-TEST-TOKEN"));
    }

    @Test
    @DisplayName("EmailTicketEventListener should skip email dispatch for guest / local email")
    void shouldSkipEmailForLocalDomain() {
        BookingConfirmedEvent event = createSampleEvent("guest-123@catchbus.local");

        emailTicketEventListener.handleBookingConfirmed(event);

        verify(notificationDispatchService, never()).dispatchOperationalSummary(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("BookingAuditEventListener should handle event without errors")
    void shouldHandleAuditLog() {
        BookingConfirmedEvent event = createSampleEvent("kamal@gmail.com");
        auditEventListener.handleBookingConfirmed(event);
        // Verify no uncaught exceptions thrown during audit logging
    }
}
