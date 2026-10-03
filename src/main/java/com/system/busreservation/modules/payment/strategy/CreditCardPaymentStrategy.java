package com.system.busreservation.modules.payment.strategy;

import com.system.busreservation.modules.payment.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy for processing Visa and Mastercard credit/debit card payments.
 * Performs Luhn algorithm validation, expiration verification, and brand detection.
 */
@Component
@Slf4j
public class CreditCardPaymentStrategy implements PaymentMethodStrategy {

    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    @Override
    public PaymentExecutionResult processPayment(PaymentContext context) {
        PaymentRequest request = context.getRequest();
        Map<String, Object> selectedCard = context.getSelectedCard();

        String brand;
        String lastFour;

        if (selectedCard != null) {
            validateExpiry((String) selectedCard.get("expiry"));
            brand = String.valueOf(selectedCard.get("cardBrand"));
            if (!List.of("VISA", "MASTERCARD").contains(brand)) {
                throw new IllegalArgumentException("Only Visa and Mastercard are accepted.");
            }
            lastFour = String.valueOf(selectedCard.get("lastFour"));
        } else {
            String cardNumber = request.cardNumber() == null ? "" : request.cardNumber().replaceAll("\\s+", "");
            if (cardNumber.isBlank()) {
                throw new IllegalArgumentException("Card number is required for card payments.");
            }
            if (request.cardholderName() == null || request.cardholderName().isBlank()) {
                throw new IllegalArgumentException("Cardholder name is required.");
            }
            if (request.cvv() == null || request.cvv().isBlank()) {
                throw new IllegalArgumentException("CVV is required.");
            }
            validateCard(cardNumber, request.expiry());
            brand = detectCardBrand(cardNumber);
            lastFour = cardNumber.substring(cardNumber.length() - 4);
        }

        log.info("[Strategy:CreditCard] Authorized payment of LKR {} with {} ending in {}",
                context.getTotalAmount(), brand, lastFour);

        return PaymentExecutionResult.builder()
                .successful(true)
                .paymentMethod("CARD")
                .cardBrand(brand)
                .cardLastFour(lastFour)
                .paymentStatus("SUCCESS")
                .message("Card payment authorized successfully via " + brand)
                .build();
    }

    @Override
    public boolean supports(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return true; // Default fallback strategy
        }
        String normalized = paymentMethod.trim().toUpperCase();
        return "CARD".equals(normalized) || "CREDIT_CARD".equals(normalized) || "DEBIT_CARD".equals(normalized);
    }

    @Override
    public String getDisplayName() {
        return "Credit / Debit Card";
    }

    public void validateCard(String number, String expiry) {
        if (!number.matches("\\d{16}")) {
            throw new IllegalArgumentException("The card number must contain exactly 16 digits.");
        }
        detectCardBrand(number);
        if (!passesLuhnCheck(number)) {
            throw new IllegalArgumentException("Enter a valid Visa or Mastercard number.");
        }
        validateExpiry(expiry);
    }

    public void validateExpiry(String expiry) {
        if (expiry == null || expiry.isBlank()) {
            throw new IllegalArgumentException("Card expiry date is required.");
        }
        try {
            YearMonth exp = YearMonth.parse(expiry.trim(), EXPIRY_FORMATTER);
            if (exp.isBefore(YearMonth.now())) {
                throw new IllegalArgumentException("The card has expired.");
            }
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Expiry must use MM/YY.");
        }
    }

    public String detectCardBrand(String card) {
        if (card.matches("4\\d{15}")) return "VISA";
        if (card.matches("5[1-5]\\d{14}")) return "MASTERCARD";
        if (card.matches("2\\d{15}")) {
            int prefix = Integer.parseInt(card.substring(0, 4));
            if (prefix >= 2221 && prefix <= 2720) return "MASTERCARD";
        }
        throw new IllegalArgumentException("Only standard Visa and Mastercard numbers are accepted.");
    }

    private boolean passesLuhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.digit(number.charAt(i), 10);
            if (digit < 0) return false;
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
