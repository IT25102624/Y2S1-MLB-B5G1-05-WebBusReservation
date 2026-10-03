package com.system.busreservation.modules.payment.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory pattern implementation for resolving the appropriate PaymentMethodStrategy at runtime.
 * Adheres to the Open/Closed Principle (OCP) - new payment gateways can be registered
 * without modifying client services.
 */
@Component
public class PaymentStrategyFactory {

    private final List<PaymentMethodStrategy> strategies;
    private final CreditCardPaymentStrategy defaultStrategy;

    public PaymentStrategyFactory(List<PaymentMethodStrategy> strategies) {
        this.strategies = strategies != null ? strategies : List.of();
        this.defaultStrategy = this.strategies.stream()
                .filter(s -> s instanceof CreditCardPaymentStrategy)
                .map(s -> (CreditCardPaymentStrategy) s)
                .findFirst()
                .orElseGet(CreditCardPaymentStrategy::new);
    }

    /**
     * Resolves the matching payment strategy based on the method name.
     *
     * @param paymentMethod e.g. "CARD", "CASH", "PAYHERE"
     * @return the resolved PaymentMethodStrategy, or the default CreditCardPaymentStrategy
     */
    public PaymentMethodStrategy getStrategy(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return defaultStrategy;
        }

        return strategies.stream()
                .filter(strategy -> !(strategy instanceof CreditCardPaymentStrategy) && strategy.supports(paymentMethod))
                .findFirst()
                .orElse(defaultStrategy);
    }

    /**
     * Returns all registered payment strategies.
     */
    public List<PaymentMethodStrategy> getAllStrategies() {
        return List.copyOf(strategies);
    }
}
