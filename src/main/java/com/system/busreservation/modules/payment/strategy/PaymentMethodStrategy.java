package com.system.busreservation.modules.payment.strategy;

/**
 * GoF Strategy Pattern interface for bus reservation payment methods.
 * Enables the payment processing algorithm to vary independently from the client service.
 */
public interface PaymentMethodStrategy {

    /**
     * Validates payment inputs and executes payment processing logic.
     *
     * @param context the payment context containing transaction details and passenger request
     * @return PaymentExecutionResult containing execution status, brand and metadata
     */
    PaymentExecutionResult processPayment(PaymentContext context);

    /**
     * Determines whether this strategy can handle the given payment method identifier.
     *
     * @param paymentMethod payment method identifier (e.g., "CARD", "CASH", "PAYHERE")
     * @return true if this strategy supports the method
     */
    boolean supports(String paymentMethod);

    /**
     * Returns the human-readable display name of the payment method.
     *
     * @return display label
     */
    String getDisplayName();
}
