package lk.catchbus.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;
    public BookingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String,Object> board(long userId, String reference, String remarks) {
        String digits = reference.replaceAll("\\D+", "");
        if (digits.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking reference.");
        long bookingId;
        try { bookingId = Long.parseLong(digits); } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking reference.");
        }
        var booking = jdbc.query("""
            SELECT booking_status, trip_id FROM bookings WHERE booking_id=?""",
            rs -> rs.next() ? Map.of("status", rs.getString("booking_status"), "tripId", rs.getLong("trip_id")) : null,
            bookingId);
        if (booking == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unrecognized QR or booking reference.");
        String status = (String) booking.get("status");
        if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status))
            throw new ResponseStatusException(HttpStatus.GONE, "This ticket has been cancelled.");
        if ("BOARDED".equalsIgnoreCase(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate QR scan: this ticket has already been boarded.");
        jdbc.update("UPDATE bookings SET booking_status='BOARDED' WHERE booking_id=?", bookingId);
        Integer boarded = jdbc.queryForObject("""
            SELECT COUNT(*) FROM bookings WHERE trip_id=? AND booking_status='BOARDED'""",
            Integer.class, booking.get("tripId"));
        return Map.of("bookingReference", reference, "bookingId", bookingId, "boardedCount", boarded == null ? 0 : boarded,
                "remarks", remarks == null ? "" : remarks,
                "verifiedAt", Instant.now());
    }
}
