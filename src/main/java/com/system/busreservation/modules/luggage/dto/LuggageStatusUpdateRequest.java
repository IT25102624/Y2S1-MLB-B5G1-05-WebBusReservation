package com.system.busreservation.modules.luggage.dto;

import com.system.busreservation.model.enums.LuggageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuggageStatusUpdateRequest {

    @NotNull(message = "Luggage status is required")
    private LuggageStatus status;
}
