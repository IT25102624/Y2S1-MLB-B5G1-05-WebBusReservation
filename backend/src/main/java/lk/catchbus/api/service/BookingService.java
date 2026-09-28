package lk.catchbus.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;
    public BookingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String,Object> board(long userId, String reference, String remarks) {
        var booking = findTicket(reference);
        if (booking == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unrecognized QR or booking reference.");
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(booking.get("approvalStatus"))))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrator approval is required before passenger details can be shown.");
        long bookingId = ((Number) booking.get("bookingId")).longValue();
        String status = (String) booking.get("status");
        if ("REFUNDED".equalsIgnoreCase(String.valueOf(booking.get("verificationStatus"))))
            throw new ResponseStatusException(HttpStatus.GONE, "Refund completed. This QR ticket is no longer valid for boarding.");
        if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status))
            throw new ResponseStatusException(HttpStatus.GONE, "This ticket has been cancelled.");
        if ("BOARDED".equalsIgnoreCase(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate QR scan: this ticket has already been boarded.");
        jdbc.update("UPDATE bookings SET booking_status='BOARDED' WHERE booking_id=?", bookingId);
        jdbc.update("UPDATE tickets SET verification_status='VERIFIED', verified_at=SYSUTCDATETIME(), verified_by=? WHERE booking_id=?",
                "Conductor #" + userId, bookingId);
        Integer boarded = jdbc.queryForObject("""
            SELECT COUNT(*) FROM bookings WHERE trip_id=? AND booking_status='BOARDED'""",
            Integer.class, booking.get("tripId"));
        Map<String,Object> result = new LinkedHashMap<>(booking);
        result.put("bookingReference", reference);
        result.put("boardedCount", boarded == null ? 0 : boarded);
        result.put("remarks", remarks == null ? "" : remarks);
        result.put("verifiedAt", Instant.now());
        return result;
    }

    public Map<String,Object> adminTicket(String reference) {
        Map<String,Object> ticket = findTicket(reference);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unrecognized QR or booking reference.");
        return ticket;
    }

    public Map<String,Object> approve(String reference, String adminName) {
        Map<String,Object> ticket = adminTicket(reference);
        jdbc.update("""
                UPDATE tickets SET approval_status='APPROVED', approved_at=SYSUTCDATETIME(), approved_by=?
                WHERE booking_id=?
                """, adminName, ticket.get("bookingId"));
        return adminTicket(reference);
    }

    private Map<String,Object> findTicket(String reference) {
        String value = reference == null ? "" : reference.trim();
        String token = value.startsWith("CATCHBUS:TICKET:") ? value.substring("CATCHBUS:TICKET:".length()) : value;
        String digits = value.replaceAll("\\D+", "");
        Long bookingId = null;
        try { if (!digits.isBlank() && digits.length() < 19) bookingId = Long.parseLong(digits); } catch (NumberFormatException ignored) {}
        List<Map<String,Object>> rows = jdbc.query("""
                SELECT b.booking_id, b.booking_status, b.trip_id, b.total_amount,
                       t.ticket_token, t.passenger_name, t.passenger_phone, t.boarding_point, t.destination_point,
                       t.approval_status, t.refund_fee_paid, t.verification_status,
                       r.source_city, r.destination_city, bu.bus_number
                FROM bookings b JOIN tickets t ON t.booking_id=b.booking_id
                JOIN trips tr ON tr.trip_id=b.trip_id JOIN routes r ON r.route_id=tr.route_id
                JOIN buses bu ON bu.bus_id=tr.bus_id
                WHERE t.ticket_token=? OR b.booking_id=?
                """, (rs, row) -> {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("bookingId", rs.getLong("booking_id")); item.put("status", rs.getString("booking_status"));
            item.put("tripId", rs.getLong("trip_id")); item.put("totalAmount", rs.getBigDecimal("total_amount"));
            item.put("ticketToken", rs.getString("ticket_token")); item.put("passengerName", rs.getString("passenger_name"));
            item.put("passengerPhone", rs.getString("passenger_phone")); item.put("boardingPoint", rs.getString("boarding_point"));
            item.put("destinationPoint", rs.getString("destination_point")); item.put("approvalStatus", rs.getString("approval_status"));
            item.put("refundFeePaid", rs.getBoolean("refund_fee_paid")); item.put("verificationStatus", rs.getString("verification_status"));
            item.put("sourceCity", rs.getString("source_city")); item.put("destinationCity", rs.getString("destination_city"));
            item.put("busNumber", rs.getString("bus_number")); return item;
        }, token, bookingId == null ? -1L : bookingId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
