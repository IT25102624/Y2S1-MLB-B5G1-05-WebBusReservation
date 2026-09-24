package lk.catchbus.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import lk.catchbus.api.repository.StaffRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final StaffRepository staff;

    public AdminController(JdbcTemplate jdbc, PasswordEncoder encoder, StaffRepository staff) {
        this.jdbc = jdbc; this.encoder = encoder; this.staff = staff;
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> messages() {
        return staff.messages(0, true);
    }

    @GetMapping("/users")
    public List<Map<String,Object>> users(@RequestParam(required=false) String role) {
        String sql = """
            SELECT u.user_id id, u.full_name fullName, u.email, u.role,
                   COALESCE(sp.employee_id,'') employeeId,
                   CASE WHEN sp.user_id IS NOT NULL AND sp.active=0 THEN 'SUSPENDED' ELSE 'ACTIVE' END status
            FROM users u
            LEFT JOIN staff_profiles sp ON sp.user_id=u.user_id
            """;
        if (role != null && !role.isBlank()) sql += " WHERE u.role=? ";
        sql += " ORDER BY u.user_id DESC";
        return role == null || role.isBlank()
                ? jdbc.queryForList(sql)
                : jdbc.queryForList(sql, role.toUpperCase());
    }

    @PatchMapping("/users/{id}/rbac")
    public Map<String,Object> updateStatus(@PathVariable long id, @RequestBody Map<String,String> body) {
        String status = body.getOrDefault("status", "ACTIVE").toUpperCase();
        if (!Set.of("ACTIVE","SUSPENDED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or SUSPENDED.");
        int updated = jdbc.update("UPDATE staff_profiles SET active=? WHERE user_id=?", "ACTIVE".equals(status), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only staff profiles can be suspended from this screen.");
        return Map.of("success", true, "status", status);
    }

    @PatchMapping("/users/{id}/position")
    public Map<String,Object> updatePosition(@PathVariable long id, @RequestBody Map<String,String> body) {
        String role = body.getOrDefault("role", "").toUpperCase();
        if (!Set.of("CONDUCTOR", "DRIVER").contains(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position must be CONDUCTOR or DRIVER.");

        int updated = jdbc.update("UPDATE users SET role=? WHERE user_id=? AND role IN ('CONDUCTOR','DRIVER')", role, id);
        if (updated == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only driver and conductor accounts can be reassigned.");

        jdbc.update("UPDATE staff_assignments SET role=? WHERE user_id=? AND active=1", role, id);
        return Map.of("success", true, "role", role);
    }

    @DeleteMapping("/users/{id}")
    public Map<String,Object> deactivate(@PathVariable long id) {
        int updated = jdbc.update("UPDATE staff_profiles SET active=0 WHERE user_id=?", id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only staff profiles can be deactivated from this screen.");
        return Map.of("success", true);
    }

    @PostMapping("/users/staff")
    public Map<String,Object> createStaff(@Valid @RequestBody StaffCreateRequest r) {
        if (!r.role.equals("CONDUCTOR") && !r.role.equals("DRIVER"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be CONDUCTOR or DRIVER.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class, r.email) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM staff_profiles WHERE employee_id=?", Integer.class, r.employeeId) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID already exists.");

        jdbc.update("""
                       INSERT INTO users(full_name,email,phone_number,password_hash,role,created_at)
                       VALUES(?,?,?,?,?,SYSUTCDATETIME())""",
                r.fullName.trim(), r.email.trim().toLowerCase(), r.phoneNumber.trim(),
                encoder.encode(r.temporaryPassword), r.role);
        long userId = jdbc.queryForObject("SELECT TOP 1 user_id FROM users WHERE email=? ORDER BY user_id DESC", Long.class, r.email);
        String token = r.role + "-" + UUID.randomUUID();

        jdbc.update("INSERT INTO staff_profiles(user_id,employee_id,qr_token,active) VALUES(?,?,?,1)",
                userId, r.employeeId.trim(), token);

        if (r.assignedBusPlate != null && !r.assignedBusPlate.isBlank()) {
            Long busId = null;
            var ids = jdbc.query("SELECT bus_id FROM buses WHERE bus_number=?",
                    rs -> rs.next() ? rs.getLong(1) : null, r.assignedBusPlate.trim());
            busId = ids;
            if (busId != null) {
                Long tripId = jdbc.query("""
                    SELECT TOP 1 trip_id FROM trips
                    WHERE bus_id=? AND departure_time >= SYSUTCDATETIME() AND trip_status <> 'CANCELLED'
                    ORDER BY departure_time ASC""",
                    rs -> rs.next() ? rs.getLong(1) : null, busId);
                if (tripId != null) {
                    jdbc.update("""
                                  INSERT INTO staff_assignments(user_id,trip_id,role,assigned_from,active)
                                  VALUES(?,?,?,SYSUTCDATETIME(),1)""", userId, tripId, r.role);
                }
            }
        }
        return Map.of("success", true, "userId", userId, "employeeId", r.employeeId, "qrToken", token,
                "qrPayload", r.role + ":" + token);
    }

    public record StaffCreateRequest(
            @NotBlank @Size(max=100) String fullName,
            @NotBlank @Email @Size(max=150) String email,
            @NotBlank @Size(max=20) String phoneNumber,
            @NotBlank @Size(max=50) String employeeId,
            @NotBlank String role,
            String assignedBusPlate,
            @NotBlank @Size(min=8, max=100) String temporaryPassword) {}
}
