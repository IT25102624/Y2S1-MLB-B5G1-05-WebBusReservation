package lk.catchbus.api.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class StaffDtos {
    private StaffDtos() {}

    public record LocationRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            BigDecimal speedKmh,
            BigDecimal headingDegrees) {}

    public record BoardRequest(@NotBlank String bookingReference, String remarks) {}

    public record Dashboard(
            Object currentSchedule,
            Object previousBus,
            Object previousBusLiveLocation) {}
}
