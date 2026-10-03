package com.system.busreservation.modules.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaymentRequest(
        @NotNull Long tripId,
        @NotEmpty @Size(max = 8) List<@NotNull Long> seatIds,
        @NotBlank @Size(max = 100) String passengerName,
        @NotBlank @Pattern(regexp = "^[+0-9][0-9 ]{8,14}$", message = "Enter a valid phone number") String phoneNumber,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String boardingPoint,
        @NotBlank @Size(max = 100) String destinationPoint,
        @Size(max = 100) String cardholderName,
        @Pattern(regexp = "^[0-9 ]*$", message = "Card number may contain only digits and spaces") String cardNumber,
        @Pattern(regexp = "^((0[1-9]|1[0-2])/[0-9]{2})?$", message = "Expiry must use MM/YY") String expiry,
        @Pattern(regexp = "^([0-9]{3,4})?$", message = "CVV must contain 3 or 4 digits") String cvv,
        Long savedCardId,
        Boolean saveCard,
        Boolean refundProtection,
        @NotNull(message = "You must accept the terms and conditions") Boolean termsAccepted,
        String paymentMethod
) {
    /**
     * Backward-compatible 15-argument constructor defaulting paymentMethod to "CARD".
     */
    public PaymentRequest(
            Long tripId,
            List<Long> seatIds,
            String passengerName,
            String phoneNumber,
            String email,
            String boardingPoint,
            String destinationPoint,
            String cardholderName,
            String cardNumber,
            String expiry,
            String cvv,
            Long savedCardId,
            Boolean saveCard,
            Boolean refundProtection,
            Boolean termsAccepted
    ) {
        this(tripId, seatIds, passengerName, phoneNumber, email, boardingPoint, destinationPoint,
                cardholderName, cardNumber, expiry, cvv, savedCardId, saveCard, refundProtection, termsAccepted, "CARD");
    }
}
