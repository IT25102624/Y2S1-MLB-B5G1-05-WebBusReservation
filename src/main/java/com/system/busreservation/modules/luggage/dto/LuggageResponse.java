package com.system.busreservation.modules.luggage.dto;

import com.system.busreservation.model.Luggage;
import com.system.busreservation.model.enums.LuggageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuggageResponse {

    private Long id;
    private String tagNumber;
    private Integer bagQuantity;
    private String description;
    private LuggageStatus status;
    private LocalDateTime createdAt;
    private Long bookingId;
    private Long userId;
    private String passengerName;
    private String routeInfo;

    public static LuggageResponse fromEntity(Luggage luggage) {
        String routeInfo = null;
        if (luggage.getBooking() != null && luggage.getBooking().getTrip() != null && luggage.getBooking().getTrip().getRoute() != null) {
            routeInfo = luggage.getBooking().getTrip().getRoute().getSourceCity() + " to " +
                    luggage.getBooking().getTrip().getRoute().getDestinationCity();
        }

        return LuggageResponse.builder()
                .id(luggage.getLuggageId())
                .tagNumber(luggage.getTagNumber())
                .bagQuantity(luggage.getBagQuantity())
                .description(luggage.getDescription())
                .status(luggage.getStatus())
                .createdAt(luggage.getCreatedAt())
                .bookingId(luggage.getBooking() != null ? luggage.getBooking().getBookingId() : null)
                .userId(luggage.getUser() != null ? luggage.getUser().getUserId() : null)
                .passengerName(luggage.getUser() != null ? luggage.getUser().getFullName() : null)
                .routeInfo(routeInfo)
                .build();
    }
}
