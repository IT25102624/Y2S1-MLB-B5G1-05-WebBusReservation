package com.system.busreservation.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.system.busreservation.model.enums.TripStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing scheduled trip (rescheduling, fare update, status change).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateRequest {

    @NotNull(message = "Departure time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "1.00", message = "Base fare must be at least 1.00")
    private BigDecimal baseFare;

    @NotNull(message = "Trip status is required")
    private TripStatus tripStatus;
}
