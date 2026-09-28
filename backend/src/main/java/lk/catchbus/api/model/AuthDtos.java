package lk.catchbus.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            String qrToken) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 20) String phoneNumber,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record AuthUser(long id, String fullName, String email, String role,
                           String employeeId, String assignedBusPlate) {}

    public record AuthResponse(String accessToken, AuthUser user, Object dashboard) {}
}
