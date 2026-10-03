package com.system.busreservation.modules.payment.strategy;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulates the execution outcome returned by a PaymentMethodStrategy.
 */
@Getter
@Builder
public class PaymentExecutionResult {
    private final boolean successful;
    private final String paymentMethod;
    private final String cardBrand;
    private final String cardLastFour;
    private final String paymentStatus;
    private final String message;
}
