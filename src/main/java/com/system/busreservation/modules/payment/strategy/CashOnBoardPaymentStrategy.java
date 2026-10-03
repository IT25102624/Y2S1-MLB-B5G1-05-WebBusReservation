package com.system.busreservation.modules.payment.strategy;

import com.system.busreservation.modules.payment.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy for Cash on Board payments.
 * Allows passengers to reserve their seats online and pay the conductor in cash upon boarding.
 */
@Component
@Slf4j
public class CashOnBoardPaymentStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentExecutionResult processPayment(PaymentContext context) {
        PaymentRequest request = context.getRequest();

        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new IllegalArgumentException("A valid contact phone number is required for Cash on Board reservations.");
        }

        log.info("[Strategy:CashOnBoard] Reserved booking #{} for cash collection of LKR {} from passenger [{}]",
                context.getBookingId(), context.getTotalAmount(), request.passengerName());

        return PaymentExecutionResult.builder()
                .successful(true)
                .paymentMethod("CASH")
                .cardBrand("CASH")
                .cardLastFour("0000")
                .paymentStatus("PENDING_CASH")
                .message("Seat confirmed for cash payment to the bus conductor upon boarding.")
                .build();
    }

    @Override
    public boolean supports(String paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }
        String normalized = paymentMethod.trim().toUpperCase();
        return "CASH".equals(normalized) || "CASH_ON_BOARD".equals(normalized) || "ON_BOARD".equals(normalized);
    }

    @Override
    public String getDisplayName() {
        return "Cash on Board (Conductor Payment)";
    }
}
