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
 * Request DTO for creating and scheduling a new bus trip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateRequest {

    @NotNull(message = "Route ID is required")
    private Long routeId;

    @NotNull(message = "Bus ID is required")
    private Long busId;

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

    @Builder.Default
    private TripStatus tripStatus = TripStatus.SCHEDULED;

    @Builder.Default
    private Boolean isRecurring = false;

    private String recurrenceType; // DAILY, WEEKDAYS, WEEKENDS

    private Integer repeatDays; // e.g. 7, 14, 30
}
