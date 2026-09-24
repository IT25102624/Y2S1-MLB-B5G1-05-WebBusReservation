package lk.catchbus.api.service;

import lk.catchbus.api.model.AuthDtos.*;
import lk.catchbus.api.repository.StaffRepository;
import lk.catchbus.api.repository.UserRepository;
import lk.catchbus.api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AuthService {
    private final UserRepository users;
    private final StaffRepository staff;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JdbcTemplate jdbc;

    public AuthService(UserRepository users, StaffRepository staff, PasswordEncoder encoder, JwtService jwt, JdbcTemplate jdbc) {
        this.users = users; this.staff = staff; this.encoder = encoder; this.jwt = jwt; this.jdbc = jdbc;
    }

    public AuthResponse passengerRegister(RegisterRequest r) {
        if (users.findByEmail(r.email()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        long id = users.insertPassenger(r.fullName().trim(), r.email().trim().toLowerCase(),
                r.phoneNumber().trim(), encoder.encode(r.password()));
        return loginByUserId(id, null, false);
    }

    public AuthResponse passengerLogin(LoginRequest r) {
        var user = users.findByEmail(r.email()).orElseThrow(() -> invalidUsername());
        if (!"PASSENGER".equalsIgnoreCase((String)user.get("role"))) throw invalidUsername();
        if (!encoder.matches(r.password(), (String)user.get("password_hash"))) throw invalidPassword();
        return response(user, null);
    }

    public AuthResponse staffLogin(LoginRequest r) {
        var user = users.findByEmail(r.email()).orElseThrow(() -> invalidUsername());
        String role = ((String) user.get("role")).toUpperCase();
        if (!role.equals("CONDUCTOR") && !role.equals("DRIVER")) throw invalidUsername();
        if (!encoder.matches(r.password(), (String) user.get("password_hash"))) throw invalidPassword();
        return response(user, null);
    }

    public AuthResponse adminLogin(LoginRequest r) {
        var user = users.findByEmail(r.email()).orElseThrow(() -> invalidUsername());
        if (!"ADMIN".equalsIgnoreCase((String)user.get("role"))) throw invalidUsername();
        if (!encoder.matches(r.password(), (String)user.get("password_hash"))) throw invalidPassword();
        return response(user, null);
    }

    public AuthResponse staffLoginByQr(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) throw invalid();
        var staffProfile = staff.findStaffByQr(qrToken).orElseThrow(() -> invalid());
        String role = ((String) staffProfile.get("role")).toUpperCase();
        if (!role.equals("CONDUCTOR") && !role.equals("DRIVER")) throw invalid();
        return loginByUserId((long) staffProfile.get("user_id"), qrToken, true);
    }

    private AuthResponse loginByUserId(long id, String qr, boolean staffOnly) {
        var user = users.findByEmail(jdbc.queryForObject("SELECT email FROM users WHERE user_id=?", String.class, id)).orElseThrow();
        return response(user, qr);
    }

    private AuthResponse response(Map<String,Object> user, String qr) {
        long id = (long)user.get("user_id");
        String role = (String)user.get("role");
        var profile = staff.profile(id).orElse(null);
        String employeeId = profile == null ? null : (String)profile.get("employee_id");
        String bus = staff.assignment(id).map(a -> (String)a.get("busNumber")).orElse(null);
        AuthUser dto = new AuthUser(id, (String)user.get("full_name"), (String)user.get("email"), role, employeeId, bus);
        Object dashboard = switch (role.toUpperCase()) {
            case "CONDUCTOR", "DRIVER" -> dashboardFor(id);
            case "PASSENGER" -> passengerSummary(id);
            default -> null;
        };
        return new AuthResponse(jwt.createToken(id, (String)user.get("email"), role), dto, dashboard);
    }

    public Map<String, Object> passengerSummary(long userId) {
        Integer completed = jdbc.queryForObject("""
            SELECT COUNT(*) FROM bookings b
            JOIN trips t ON t.trip_id=b.trip_id
            WHERE b.user_id=? AND b.booking_status IN ('BOARDED','COMPLETED')
              AND t.arrival_time <= SYSUTCDATETIME()""", Integer.class, userId);
        int trips = completed == null ? 0 : completed;
        int discount = trips >= 20 ? 15 : trips >= 10 ? 10 : trips >= 5 ? 5 : 0;
        int nextTarget = trips < 5 ? 5 : trips < 10 ? 10 : trips < 20 ? 20 : 20;
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("completedTrips", trips);
        result.put("discountPercent", discount);
        result.put("nextDiscountTarget", nextTarget);
        result.put("nextDiscountPercent", trips >= 20 ? 15 : trips >= 10 ? 15 : trips >= 5 ? 10 : 5);
        result.put("tripsToNextDiscount", trips >= 20 ? 0 : nextTarget - trips);
        result.put("recentTrips", jdbc.queryForList("""
            SELECT TOP 10 b.booking_id bookingId, b.total_amount totalAmount,
                   b.booking_status bookingStatus, b.booking_time bookingTime,
                   r.source_city sourceCity, r.destination_city destinationCity,
                   t.departure_time departureTime
            FROM bookings b
            JOIN trips t ON t.trip_id=b.trip_id
            JOIN routes r ON r.route_id=t.route_id
            WHERE b.user_id=?
            ORDER BY b.booking_time DESC""", userId));
        return result;
    }

    public Map<String,Object> dashboardFor(long userId) {
        var current = staff.assignment(userId);
        if (current.isEmpty()) {
            java.util.Map<String,Object> empty = new java.util.LinkedHashMap<>();
            empty.put("currentSchedule", null);
            empty.put("previousBus", null);
            empty.put("previousBusLiveLocation", null);
            return empty;
        }
        long tripId = (long)current.get().get("tripId");
        var previous = staff.previousTrip(tripId);
        java.util.Map<String,Object> result = new java.util.LinkedHashMap<>();
        result.put("currentSchedule", current.get());
        result.put("previousBus", previous.orElse(null));
        result.put("previousBusLiveLocation", previous.flatMap(p -> staff.latestLocation((long)p.get("tripId"))).orElse(null));
        return result;
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }

    private ResponseStatusException invalidUsername() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or email.");
    }

    private ResponseStatusException invalidPassword() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password.");
    }
}
