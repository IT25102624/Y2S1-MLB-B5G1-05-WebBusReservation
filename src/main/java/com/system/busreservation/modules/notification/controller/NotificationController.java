package com.system.busreservation.modules.notification.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.notification.dto.NotificationResponse;
import com.system.busreservation.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Notification and Alerts Management.
 * Module 5: Assigned to Wanniarachchi P. S. (IT25103670).
 */
@RestController
@RequestMapping({"/notifications", "/api/v1/notifications", "/api/notifications"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForUser(@PathVariable Long userId) {
        log.info("REST request to fetch notifications for user: {}", userId);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable Long userId) {
        log.info("REST request to fetch unread notifications for user: {}", userId);
        return ResponseEntity.ok(notificationService.getUnreadNotificationsForUser(userId));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        log.info("REST request to mark notification {} as read", notificationId);
        boolean success = notificationService.markAsRead(notificationId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder().success(false).message("Notification not found").build());
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        log.info("REST request to mark all notifications as read for user: {}", userId);
        boolean success = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(
                success ? "All notifications marked as read" : "No unread notifications found", null));
    }
}
