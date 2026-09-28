package lk.catchbus.api.controller;

import jakarta.validation.Valid;
import lk.catchbus.api.model.StaffDtos.*;
import lk.catchbus.api.repository.StaffRepository;
import lk.catchbus.api.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final AuthService auth;
    private final StaffRepository staff;
    public StaffController(AuthService auth, StaffRepository staff) { this.auth = auth; this.staff = staff; }

    @GetMapping("/dashboard")
    public Dashboard dashboard(Authentication authentication) {
        long userId = (long) authentication.getPrincipal();
        var d = auth.dashboardFor(userId);
        return new Dashboard(d.get("currentSchedule"), d.get("previousBus"), d.get("previousBusLiveLocation"));
    }

    @PostMapping("/location")
    public Object updateLocation(@Valid @RequestBody LocationRequest request, Authentication authentication) {
        long userId = (long) authentication.getPrincipal();
        var assignment = staff.currentAssignmentForUser(userId).orElseThrow();
        staff.saveLocation(userId, (long)assignment.get("tripId"), request.latitude(), request.longitude(),
                request.speedKmh(), request.headingDegrees());
        return java.util.Map.of("success", true, "message", "Bus location updated.");
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> messages(Authentication authentication) {
        long userId = (long) authentication.getPrincipal();
        return staff.messages(userId, false);
    }

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@Valid @RequestBody StaffMessageRequest request,
                                           Authentication authentication) {
        long userId = (long) authentication.getPrincipal();
        staff.saveMessage(userId, request.subject(), request.message(), request.urgent());
        return Map.of("success", true, "message", "Message sent to operations.");
    }

    public record StaffMessageRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String subject,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 2000) String message,
            boolean urgent) {}
}
