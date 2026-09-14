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

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getNotificationId())
                .userId(notification.getUser() != null ? notification.getUser().getUserId() : null)
                .bookingId(notification.getBooking() != null ? notification.getBooking().getBookingId() : null)
                .message(notification.getMessage())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .build();
    }
}
