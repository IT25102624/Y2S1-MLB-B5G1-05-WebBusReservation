package com.system.busreservation.modules.notification.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Booking;
import com.system.busreservation.model.Notification;
import com.system.busreservation.model.User;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.notification.dto.NotificationResponse;
import com.system.busreservation.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        log.info("Fetching all notifications for user ID: {}", userId);
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotificationsForUser(Long userId) {
        log.info("Fetching unread notifications for user ID: {}", userId);
        return notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Transactional
    public boolean markAsRead(Long notificationId) {
        log.info("Marking notification ID {} as read", notificationId);
        return notificationRepository.findById(notificationId)
                .map(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user ID: {}", userId);
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        return updated > 0;
    }

    @Transactional
    public Notification sendNotification(User user, Booking booking, String message, String type) {
        log.info("Dispatching notification to user ID: {} - Type: {}", user.getUserId(), type);
        Notification notification = Notification.builder()
                .user(user)
                .booking(booking)
                .message(message)
                .type(type)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendNotification(Long userId, Long bookingId, String message, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        Booking booking = null;
        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId).orElse(null);
        }
        return sendNotification(user, booking, message, type);
    }
}
