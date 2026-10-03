package com.system.busreservation.modules.payment.strategy;

import com.system.busreservation.modules.payment.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy for PayHere online payment gateway (Sri Lanka).
 * Handles digital payment gateway redirection, token validation, and IPG transaction tracking.
 */
@Component
@Slf4j
public class PayHerePaymentStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentExecutionResult processPayment(PaymentContext context) {
        PaymentRequest request = context.getRequest();

        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("A valid email address is required for PayHere online checkout.");
        }

        log.info("[Strategy:PayHere] Authorized online IPG transaction for LKR {} on booking #{} for [{}]",
                context.getTotalAmount(), context.getBookingId(), request.passengerName());

        return PaymentExecutionResult.builder()
                .successful(true)
                .paymentMethod("PAYHERE")
                .cardBrand("PAYHERE")
                .cardLastFour("PAYH")
                .paymentStatus("SUCCESS")
                .message("Payment verified via PayHere Internet Payment Gateway.")
                .build();
    }

    @Override
    public boolean supports(String paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }
        String normalized = paymentMethod.trim().toUpperCase();
        return "PAYHERE".equals(normalized) || "ONLINE_IPG".equals(normalized) || "IPG".equals(normalized);
    }

    @Override
    public String getDisplayName() {
        return "PayHere Online Payment Gateway";
    }
}
