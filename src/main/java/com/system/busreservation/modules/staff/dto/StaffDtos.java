package com.system.busreservation.modules.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class StaffDtos {

    public record LocationUpdateRequest(
            @NotNull BigDecimal latitude,
            @NotNull BigDecimal longitude,
            BigDecimal speedKmh,
            BigDecimal headingDegrees
    ) {}

    public record BoardingRequest(
            @NotBlank String bookingReference,
            String remarks
    ) {}

    public record OperationsMessageRequest(
            @NotBlank String subject,
            @NotBlank String message,
            boolean urgent
    ) {}

    public record StaffLoginRequest(
            String email,
            String password,
            String qrToken,
            String role
    ) {}
}
