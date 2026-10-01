package com.system.busreservation.modules.luggage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuggageCreateRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Bag quantity is required")
    @Min(value = 1, message = "At least 1 bag is required")
    private Integer bagQuantity;

    private String description;

    private Long userId;
}
