package com.system.busreservation.modules.feedback.dto;

import com.system.busreservation.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long feedbackId;
    private Long userId;
    private String name;
    private String type;
    private String description;
    private Integer rating;
    private LocalDate complainDate;
    private LocalTime complainTime;
    private String reply;

    public static FeedbackResponse fromEntity(Feedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .userId(feedback.getUser() != null ? feedback.getUser().getUserId() : null)
                .name(feedback.getName())
                .type(feedback.getType())
                .description(feedback.getDescription())
                .rating(feedback.getRating())
                .complainDate(feedback.getComplainDate())
                .complainTime(feedback.getComplainTime())
                .reply(feedback.getReply())
                .build();
    }
}
