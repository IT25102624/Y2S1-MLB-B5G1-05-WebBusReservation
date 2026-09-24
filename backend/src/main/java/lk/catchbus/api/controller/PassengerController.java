package lk.catchbus.api.controller;

import lk.catchbus.api.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/passenger")
public class PassengerController {
    private final AuthService auth;

    public PassengerController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(Authentication authentication) {
        return auth.passengerSummary((long) authentication.getPrincipal());
    }
}
