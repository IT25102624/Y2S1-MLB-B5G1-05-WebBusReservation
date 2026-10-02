package com.system.busreservation.modules.notification.dto;

import com.system.busreservation.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private Long bookingId;
    private String message;
    private String type;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private String route;

    public static NotificationResponse fromEntity(Notification notification) {
        String routeInfo = null;
        if (notification.getBooking() != null && notification.getBooking().getTrip() != null && notification.getBooking().getTrip().getRoute() != null) {
            routeInfo = notification.getBooking().getTrip().getRoute().getSourceCity() + " to " +
                    notification.getBooking().getTrip().getRoute().getDestinationCity();
        }

        return NotificationResponse.builder()
                .id(notification.getNotificationId())
                .userId(notification.getUser() != null ? notification.getUser().getUserId() : null)
                .bookingId(notification.getBooking() != null ? notification.getBooking().getBookingId() : null)
                .message(notification.getMessage())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .route(routeInfo)
                .build();
    }
}
