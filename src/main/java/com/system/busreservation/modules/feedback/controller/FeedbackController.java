package com.system.busreservation.modules.feedback.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.feedback.dto.FeedbackCreateRequest;
import com.system.busreservation.modules.feedback.dto.FeedbackReplyRequest;
import com.system.busreservation.modules.feedback.dto.FeedbackResponse;
import com.system.busreservation.modules.feedback.dto.FeedbackUpdateRequest;
import com.system.busreservation.modules.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Feedback and Customer Complaint Management.
 * Module 5: Assigned to Wanniarachchi P. S. (IT25103670).
 */
@RestController
@RequestMapping({"/feedback", "/api/v1/feedback", "/api/feedback"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Feedback API is operational!");
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackCreateRequest request) {
        log.info("REST request to submit feedback for user ID: {}", request.getUserId());
        FeedbackResponse created = feedbackService.createFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAllFeedback() {
        log.info("REST request to list all feedbacks");
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        log.info("REST request to fetch feedback ID: {}", id);
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackByUserId(@PathVariable Long userId) {
        log.info("REST request to fetch feedback for user ID: {}", userId);
        return ResponseEntity.ok(feedbackService.getFeedbackByUserId(userId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackByType(@PathVariable String type) {
        log.info("REST request to fetch feedback of type: {}", type);
        return ResponseEntity.ok(feedbackService.getFeedbackByType(type));
    }

    @GetMapping("/with-replies")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackWithReplies() {
        return ResponseEntity.ok(feedbackService.getFeedbackWithReplies());
    }

    @GetMapping("/without-replies")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackWithoutReplies() {
        return ResponseEntity.ok(feedbackService.getFeedbackWithoutReplies());
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<FeedbackResponse> addReply(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackReplyRequest request) {
        log.info("REST request to reply to feedback ID: {}", id);
        return ResponseEntity.ok(feedbackService.addReply(id, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestBody Map<String, Object> body) {
        log.info("REST request to update feedback ID: {}", id);

        Long effectiveUserId = userId;
        if (effectiveUserId == null && body.containsKey("userId")) {
            Object uVal = body.get("userId");
            if (uVal instanceof Number n) {
                effectiveUserId = n.longValue();
            } else if (uVal != null) {
                effectiveUserId = Long.parseLong(uVal.toString());
            }
        }

        if (effectiveUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        Integer rating = null;
        if (body.get("rating") != null && !body.get("rating").toString().isBlank()) {
            rating = Integer.parseInt(body.get("rating").toString());
        }

        FeedbackUpdateRequest request = FeedbackUpdateRequest.builder()
                .name((String) body.get("name"))
                .type((String) body.get("type"))
                .description((String) body.get("description"))
                .rating(rating)
                .build();

        return ResponseEntity.ok(feedbackService.updateOwnFeedback(id, effectiveUserId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        log.info("REST request to delete feedback ID: {}", id);
        if (userId != null) {
            feedbackService.deleteOwnFeedback(id, userId);
        } else {
            feedbackService.deleteFeedback(id);
        }
        return ResponseEntity.ok(ApiResponse.success("Feedback deleted successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        return ResponseEntity.ok(feedbackService.getStats());
    }
}
