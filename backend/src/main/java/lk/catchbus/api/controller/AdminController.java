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
import lk.catchbus.api.service.BookingService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final StaffRepository staff;
    private final BookingService bookings;

    public AdminController(JdbcTemplate jdbc, PasswordEncoder encoder, StaffRepository staff, BookingService bookings) {
        this.jdbc = jdbc; this.encoder = encoder; this.staff = staff; this.bookings = bookings;
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> messages() {
        return staff.messages(0, true);
    }

    @GetMapping("/inbox")
    public List<Map<String,Object>> inbox(org.springframework.security.core.Authentication authentication) {
        return jdbc.queryForList("""
                SELECT notification_id notificationId,booking_id bookingId,notification_type notificationType,
                       subject,message,is_read isRead,created_at createdAt
                FROM user_notifications WHERE recipient_user_id=? ORDER BY created_at DESC
                """, (long)authentication.getPrincipal());
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
                       VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)""",
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
                    WHERE bus_id=? AND departure_time >= CURRENT_TIMESTAMP AND trip_status <> 'CANCELLED'
                    ORDER BY departure_time ASC""",
                    rs -> rs.next() ? rs.getLong(1) : null, busId);
                if (tripId != null) {
                    jdbc.update("""
                                  INSERT INTO staff_assignments(user_id,trip_id,role,assigned_from,active)
                                  VALUES(?,?,?,CURRENT_TIMESTAMP,1)""", userId, tripId, r.role);
                }
            }
        }
        return Map.of("success", true, "userId", userId, "employeeId", r.employeeId, "qrToken", token,
                "qrPayload", r.role + ":" + token);
    }

    @GetMapping("/tickets/{reference}")
    public Map<String,Object> verifyTicket(@PathVariable String reference) {
        return bookings.adminTicket(reference);
    }

    @PostMapping("/tickets/{reference}/approve")
    public Map<String,Object> approveTicket(@PathVariable String reference,
                                             org.springframework.security.core.Authentication authentication) {
        return bookings.approve(reference, authentication.getName());
    }

    @GetMapping("/refunds")
    public List<Map<String,Object>> refunds() {
        return jdbc.queryForList("""
                SELECT r.refund_id refundId, r.booking_id bookingId, r.refund_amount refundAmount,
                       r.refund_status refundStatus, r.reason, r.requested_at requestedAt,
                       t.passenger_name passengerName, t.ticket_token ticketToken
                FROM refunds r JOIN tickets t ON t.booking_id=r.booking_id ORDER BY r.requested_at DESC
                """);
    }

    @GetMapping("/bookings")
    public List<Map<String,Object>> bookings() {
        return jdbc.queryForList("""
                SELECT b.booking_id bookingId,u.full_name passengerName,u.email passengerEmail,u.phone_number passengerPhone,
                       r.source_city sourceCity,r.destination_city destinationCity,bu.bus_number busNumber,tr.departure_time departureTime,
                       COALESCE(seatData.seats,CASE WHEN b.booking_status='REFUNDED' THEN 'Released' ELSE '—' END) seats,
                       b.total_amount totalAmount,b.booking_status bookingStatus,b.booking_time bookingTime,
                       COALESCE(t.approval_status,'—') approvalStatus,COALESCE(t.refund_fee_paid,0) refundFeePaid,
                       COALESCE(p.payment_status,'—') paymentStatus,
                       COALESCE(p.transaction_reference,'—') transactionReference,COALESCE(rf.refund_status,'—') refundStatus
                FROM bookings b JOIN users u ON u.user_id=b.user_id
                JOIN trips tr ON tr.trip_id=b.trip_id JOIN routes r ON r.route_id=tr.route_id JOIN buses bu ON bu.bus_id=tr.bus_id
                LEFT JOIN tickets t ON t.booking_id=b.booking_id LEFT JOIN payments p ON p.booking_id=b.booking_id
                LEFT JOIN refunds rf ON rf.booking_id=b.booking_id
                OUTER APPLY(SELECT STRING_AGG(CAST(s.seat_number AS NVARCHAR(MAX)),N', ') seats FROM booking_seats bs JOIN seats s ON s.seat_id=bs.seat_id WHERE bs.booking_id=b.booking_id) seatData
                ORDER BY b.booking_id DESC
                """);
    }

    @PostMapping("/tickets/{reference}/refunds")
    public Map<String,Object> createRefund(@PathVariable String reference, @RequestBody(required=false) Map<String,String> body,
                                            org.springframework.security.core.Authentication authentication) {
        Map<String,Object> ticket = bookings.adminTicket(reference);
        if (!Boolean.TRUE.equals(ticket.get("refundFeePaid")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This passenger did not purchase refund protection.");
        long bookingId = ((Number)ticket.get("bookingId")).longValue();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE booking_id=?", Integer.class, bookingId) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund already exists for this booking.");
        java.math.BigDecimal amount = ((java.math.BigDecimal)ticket.get("totalAmount")).subtract(new java.math.BigDecimal("150.00"));
        jdbc.update("""
                INSERT INTO refunds(booking_id,refund_amount,refund_status,reason,requested_by,requested_at)
                VALUES(?,?,'REQUESTED',?,?,SYSUTCDATETIME())
                """, bookingId, amount, body == null ? "Passenger cancellation" : body.getOrDefault("reason", "Passenger cancellation"), authentication.getName());
        return Map.of("success", true, "bookingId", bookingId, "refundAmount", amount, "refundStatus", "REQUESTED", "processingTime", "7 business days");
    }

    @PatchMapping("/refunds/{id}")
    public Map<String,Object> updateRefund(@PathVariable long id, @RequestBody Map<String,String> body,
                                            org.springframework.security.core.Authentication authentication) {
        String action = body.getOrDefault("action", "").toUpperCase();
        int updated;
        if ("APPROVE".equals(action)) {
            updated = jdbc.update("UPDATE refunds SET refund_status='REFUND',approved_by=?,approved_at=SYSUTCDATETIME(),processed_at=SYSUTCDATETIME(),admin_notes='Approved from admin' WHERE refund_id=? AND refund_status='REQUESTED'", authentication.getName(), id);
            if (updated > 0) {
                Map<String,Object> refund = jdbc.queryForMap("SELECT r.booking_id,b.user_id FROM refunds r JOIN bookings b ON b.booking_id=r.booking_id WHERE r.refund_id=?", id);
                long bookingId = ((Number)refund.get("booking_id")).longValue();
                jdbc.update("DELETE FROM payments WHERE booking_id=?", bookingId);
                jdbc.update("UPDATE bookings SET booking_status='REFUNDED' WHERE booking_id=?", bookingId);
                jdbc.update("UPDATE tickets SET verification_status='REFUNDED' WHERE booking_id=?", bookingId);
                jdbc.update("DELETE FROM booking_seats WHERE booking_id=?", bookingId);
                jdbc.update("INSERT INTO user_notifications(recipient_user_id,booking_id,notification_type,subject,message,is_read,created_at) VALUES(?,?,'REFUND_SUCCESS','Refund successful',?,0,SYSUTCDATETIME())",
                        refund.get("user_id"), bookingId, "Your refund for booking #" + bookingId + " was processed successfully. The QR ticket is no longer valid.");
            }
        }
        else if ("PROCESS".equals(action)) {
            updated = jdbc.update("UPDATE refunds SET refund_status='PROCESSED',processed_at=SYSUTCDATETIME() WHERE refund_id=? AND refund_status='APPROVED'", id);
            if (updated > 0) jdbc.update("UPDATE bookings SET booking_status='CANCELLED' WHERE booking_id=(SELECT booking_id FROM refunds WHERE refund_id=?)", id);
        }
        else if ("REJECT".equals(action)) updated = jdbc.update("UPDATE refunds SET refund_status='REJECTED',approved_by=?,approved_at=SYSUTCDATETIME() WHERE refund_id=? AND refund_status='REQUESTED'", authentication.getName(), id);
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action must be APPROVE, PROCESS or REJECT.");
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund action is not valid for its current status.");
        return jdbc.queryForMap("SELECT * FROM refunds WHERE refund_id=?", id);
    }

    @DeleteMapping("/refunds/{id}")
    public Map<String,Object> deleteRefund(@PathVariable long id) {
        if (jdbc.update("DELETE FROM refunds WHERE refund_id=? AND refund_status='REQUESTED'", id) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only requested refunds can be deleted.");
        return Map.of("success", true);
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
