package lk.catchbus.api.controller;

import jakarta.validation.Valid;
import lk.catchbus.api.model.AuthDtos.*;
import lk.catchbus.api.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/passenger/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.passengerRegister(request); }

    @PostMapping("/passenger/login")
    public AuthResponse passengerLogin(@Valid @RequestBody LoginRequest request) { return auth.passengerLogin(request); }

    @PostMapping("/staff/login")
    public AuthResponse staffLogin(@Valid @RequestBody LoginRequest request) { return auth.staffLogin(request); }

    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody LoginRequest request) { return auth.adminLogin(request); }

    @PostMapping("/staff/login/qr")
    public AuthResponse staffLoginByQr(@RequestBody java.util.Map<String, String> request) {
        return auth.staffLoginByQr(request.get("qrToken"));
    }
}
