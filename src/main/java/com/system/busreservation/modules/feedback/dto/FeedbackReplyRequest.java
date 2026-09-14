package com.system.busreservation.modules.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReplyRequest {

    @NotBlank(message = "Reply message cannot be blank")
    private String reply;
}
