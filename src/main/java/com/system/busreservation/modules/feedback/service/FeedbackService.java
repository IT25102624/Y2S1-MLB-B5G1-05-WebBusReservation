package com.system.busreservation.modules.feedback.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Feedback;
import com.system.busreservation.model.User;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.feedback.dto.FeedbackCreateRequest;
import com.system.busreservation.modules.feedback.dto.FeedbackReplyRequest;
import com.system.busreservation.modules.feedback.dto.FeedbackResponse;
import com.system.busreservation.modules.feedback.dto.FeedbackUpdateRequest;
import com.system.busreservation.modules.feedback.repository.FeedbackRepository;
import com.system.busreservation.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FeedbackResponse createFeedback(FeedbackCreateRequest request) {
        log.info("Creating feedback from user ID: {}", request.getUserId());
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", request.getUserId()));

        Feedback feedback = Feedback.builder()
                .user(user)
                .name(request.getName().trim())
                .type(request.getType().trim().toLowerCase())
                .rating(request.getRating())
                .description(request.getDescription().trim())
                .complainDate(LocalDate.now())
                .complainTime(LocalTime.now())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        return FeedbackResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedback() {
        return feedbackRepository.findAll()
                .stream()
                .map(FeedbackResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "feedbackId", id));
        return FeedbackResponse.fromEntity(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackByUserId(Long userId) {
        return feedbackRepository.findByUser_UserId(userId)
                .stream()
                .map(FeedbackResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackByType(String type) {
        return feedbackRepository.findByType(type.toLowerCase())
                .stream()
                .map(FeedbackResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackWithReplies() {
        return feedbackRepository.findWithReplies()
                .stream()
                .map(FeedbackResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackWithoutReplies() {
        return feedbackRepository.findWithoutReplies()
                .stream()
                .map(FeedbackResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FeedbackResponse updateOwnFeedback(Long id, Long userId, FeedbackUpdateRequest request) {
        log.info("Updating feedback ID {} for user ID {}", id, userId);
        Feedback feedback = feedbackRepository.findByFeedbackIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "feedbackId", id));

        if (feedback.getReply() != null && !feedback.getReply().trim().isEmpty()) {
            throw new IllegalStateException("Feedback with an official response cannot be edited.");
        }

        feedback.setName(request.getName().trim());
        feedback.setType(request.getType().trim().toLowerCase());
        feedback.setDescription(request.getDescription().trim());
        feedback.setRating(request.getRating());

        Feedback saved = feedbackRepository.save(feedback);
        return FeedbackResponse.fromEntity(saved);
    }

    @Transactional
    public FeedbackResponse addReply(Long feedbackId, FeedbackReplyRequest request) {
        log.info("Adding customer service reply to feedback ID: {}", feedbackId);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "feedbackId", feedbackId));

        feedback.setReply(request.getReply().trim());
        Feedback saved = feedbackRepository.save(feedback);

        // Send notification to the user
        try {
            notificationService.sendNotification(
                    feedback.getUser(),
                    null,
                    "Support response received for your " + feedback.getType() + ": \"" + feedback.getReply() + "\"",
                    "FEEDBACK_REPLY"
            );
        } catch (Exception e) {
            log.warn("Could not dispatch notification for feedback reply: {}", e.getMessage());
        }

        return FeedbackResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteFeedback(Long id) {
        log.info("Deleting feedback ID: {}", id);
        if (!feedbackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feedback", "feedbackId", id);
        }
        feedbackRepository.deleteById(id);
    }

    @Transactional
    public void deleteOwnFeedback(Long id, Long userId) {
        log.info("Deleting feedback ID {} by user ID {}", id, userId);
        Feedback feedback = feedbackRepository.findByFeedbackIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "feedbackId", id));
        feedbackRepository.delete(feedback);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedback", feedbackRepository.count());
        stats.put("feedbackCount", feedbackRepository.countByType("feedback"));
        stats.put("complainCount", feedbackRepository.countByType("complain"));
        stats.put("withReplies", feedbackRepository.findWithReplies().size());
        stats.put("withoutReplies", feedbackRepository.findWithoutReplies().size());
        return stats;
    }
}
