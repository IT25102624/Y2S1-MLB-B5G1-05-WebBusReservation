package com.system.busreservation.modules.payment.strategy;

import com.system.busreservation.modules.payment.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment Strategy & Factory Pattern Tests")
class PaymentStrategyTest {

    private CreditCardPaymentStrategy creditCardStrategy;
    private CashOnBoardPaymentStrategy cashStrategy;
    private PayHerePaymentStrategy payHereStrategy;
    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        creditCardStrategy = new CreditCardPaymentStrategy();
        cashStrategy = new CashOnBoardPaymentStrategy();
        payHereStrategy = new PayHerePaymentStrategy();
        factory = new PaymentStrategyFactory(List.of(creditCardStrategy, cashStrategy, payHereStrategy));
    }

    @Test
    @DisplayName("Factory should resolve CreditCardPaymentStrategy by default or when requested")
    void shouldResolveCreditCardStrategy() {
        assertEquals(creditCardStrategy, factory.getStrategy("CARD"));
        assertEquals(creditCardStrategy, factory.getStrategy("CREDIT_CARD"));
        assertEquals(creditCardStrategy, factory.getStrategy(null));
        assertEquals(creditCardStrategy, factory.getStrategy(""));
        assertEquals(creditCardStrategy, factory.getStrategy("UNKNOWN_METHOD"));
    }

    @Test
    @DisplayName("Factory should resolve CashOnBoardPaymentStrategy when requested")
    void shouldResolveCashStrategy() {
        assertEquals(cashStrategy, factory.getStrategy("CASH"));
        assertEquals(cashStrategy, factory.getStrategy("CASH_ON_BOARD"));
    }

    @Test
    @DisplayName("Factory should resolve PayHerePaymentStrategy when requested")
    void shouldResolvePayHereStrategy() {
        assertEquals(payHereStrategy, factory.getStrategy("PAYHERE"));
        assertEquals(payHereStrategy, factory.getStrategy("ONLINE_IPG"));
    }

    @Test
    @DisplayName("CreditCardPaymentStrategy should validate and approve valid Visa card")
    void shouldProcessValidVisaCard() {
        PaymentRequest request = new PaymentRequest(
                1L, List.of(101L), "Kamal Perera", "+94771234567", "kamal@test.lk",
                "Colombo", "Kandy", "Kamal Perera", "4111111111111111", "12/28", "123",
                null, false, true, true, "CARD"
        );

        PaymentContext context = PaymentContext.builder()
                .request(request)
                .totalAmount(new BigDecimal("1500.00"))
                .bookingId(10L)
                .transactionReference("PAY-TEST001")
                .build();

        PaymentExecutionResult result = creditCardStrategy.processPayment(context);

        assertTrue(result.isSuccessful());
        assertEquals("CARD", result.getPaymentMethod());
        assertEquals("VISA", result.getCardBrand());
        assertEquals("1111", result.getCardLastFour());
        assertEquals("SUCCESS", result.getPaymentStatus());
    }

    @Test
    @DisplayName("CreditCardPaymentStrategy should reject card with invalid Luhn checksum")
    void shouldRejectInvalidCardChecksum() {
        PaymentRequest request = new PaymentRequest(
                1L, List.of(101L), "Kamal Perera", "+94771234567", "kamal@test.lk",
                "Colombo", "Kandy", "Kamal Perera", "4111111111111112", "12/28", "123",
                null, false, true, true, "CARD"
        );

        PaymentContext context = PaymentContext.builder()
                .request(request)
                .totalAmount(new BigDecimal("1500.00"))
                .bookingId(10L)
                .transactionReference("PAY-TEST002")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> creditCardStrategy.processPayment(context));
        assertTrue(ex.getMessage().contains("Enter a valid Visa or Mastercard number"));
    }

    @Test
    @DisplayName("CreditCardPaymentStrategy should support saved card selection")
    void shouldProcessSavedCard() {
        PaymentRequest request = new PaymentRequest(
                1L, List.of(101L), "Kamal Perera", "+94771234567", "kamal@test.lk",
                "Colombo", "Kandy", null, null, null, null,
                5L, false, true, true, "CARD"
        );

        Map<String, Object> savedCard = Map.of(
                "cardBrand", "MASTERCARD",
                "lastFour", "5432",
                "expiry", "11/29"
        );

        PaymentContext context = PaymentContext.builder()
                .request(request)
                .selectedCard(savedCard)
                .totalAmount(new BigDecimal("2200.00"))
                .bookingId(11L)
                .transactionReference("PAY-TEST003")
                .build();

        PaymentExecutionResult result = creditCardStrategy.processPayment(context);

        assertTrue(result.isSuccessful());
        assertEquals("MASTERCARD", result.getCardBrand());
        assertEquals("5432", result.getCardLastFour());
    }

    @Test
    @DisplayName("CashOnBoardPaymentStrategy should succeed when phone number is present")
    void shouldProcessCashOnBoard() {
        PaymentRequest request = new PaymentRequest(
                1L, List.of(101L), "Nimal Silva", "+94719876543", "nimal@test.lk",
                "Colombo", "Galle", null, null, null, null,
                null, false, false, true, "CASH"
        );

        PaymentContext context = PaymentContext.builder()
                .request(request)
                .totalAmount(new BigDecimal("850.00"))
                .bookingId(12L)
                .transactionReference("PAY-CASH001")
                .build();

        PaymentExecutionResult result = cashStrategy.processPayment(context);

        assertTrue(result.isSuccessful());
        assertEquals("CASH", result.getPaymentMethod());
        assertEquals("CASH", result.getCardBrand());
        assertEquals("0000", result.getCardLastFour());
        assertEquals("PENDING_CASH", result.getPaymentStatus());
    }

    @Test
    @DisplayName("PayHerePaymentStrategy should succeed when passenger email is valid")
    void shouldProcessPayHere() {
        PaymentRequest request = new PaymentRequest(
                1L, List.of(101L), "Sunil Perera", "+94751112233", "sunil@payhere.lk",
                "Colombo", "Kandy", null, null, null, null,
                null, false, true, true, "PAYHERE"
        );

        PaymentContext context = PaymentContext.builder()
                .request(request)
                .totalAmount(new BigDecimal("3500.00"))
                .bookingId(15L)
                .transactionReference("PAY-PAYHERE01")
                .build();

        PaymentExecutionResult result = payHereStrategy.processPayment(context);

        assertTrue(result.isSuccessful());
        assertEquals("PAYHERE", result.getPaymentMethod());
        assertEquals("PAYHERE", result.getCardBrand());
        assertEquals("PAYH", result.getCardLastFour());
        assertEquals("SUCCESS", result.getPaymentStatus());
    }
}
