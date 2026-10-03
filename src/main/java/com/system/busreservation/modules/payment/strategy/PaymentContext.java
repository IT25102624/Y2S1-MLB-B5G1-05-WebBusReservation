package com.system.busreservation.modules.payment.strategy;

import com.system.busreservation.modules.payment.dto.PaymentRequest;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Context object carrying all transaction details required by a PaymentMethodStrategy.
 */
@Getter
@Builder
public class PaymentContext {
    private final PaymentRequest request;
    private final BigDecimal totalAmount;
    private final Long bookingId;
    private final String authenticatedEmail;
    private final String transactionReference;
    private final Map<String, Object> selectedCard;
}
