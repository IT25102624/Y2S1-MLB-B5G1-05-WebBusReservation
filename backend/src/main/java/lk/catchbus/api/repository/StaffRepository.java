package lk.catchbus.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class StaffRepository {
    private final JdbcTemplate jdbc;
    public StaffRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Map<String,Object>> findStaffByQr(String qrToken) {
        return jdbc.query("""
            SELECT TOP 1 sp.user_id, sp.employee_id, sp.qr_token, sp.active, u.role
            FROM staff_profiles sp
            JOIN users u ON u.user_id=sp.user_id
            WHERE sp.qr_token=? AND sp.active=1
            """, rs -> rs.next() ? Optional.of(Map.of(
                "user_id", rs.getLong("user_id"),
                "employee_id", rs.getString("employee_id"),
                "role", rs.getString("role"))) : Optional.empty(), qrToken);
    }

    public Optional<Map<String,Object>> profile(long userId) {
        return jdbc.query("""
            SELECT TOP 1 sp.employee_id, sp.qr_token, sp.active
            FROM staff_profiles sp WHERE sp.user_id=?""",
            rs -> rs.next() ? Optional.of(Map.of(
                    "employee_id", rs.getString("employee_id"),
                    "qr_token", rs.getString("qr_token"),
                    "active", rs.getBoolean("active"))) : Optional.empty(), userId);
    }

    public Optional<Map<String,Object>> assignment(long userId) {
        return jdbc.query("""
            SELECT TOP 1 sa.assignment_id, sa.trip_id, sa.role,
                   b.bus_id, b.bus_number, r.route_id, r.source_city, r.destination_city,
                   t.departure_time, t.arrival_time, t.trip_status, t.base_fare
            FROM staff_assignments sa
            JOIN trips t ON t.trip_id=sa.trip_id
            JOIN buses b ON b.bus_id=t.bus_id
            JOIN routes r ON r.route_id=t.route_id
            WHERE sa.user_id=? AND sa.active=1
              AND sa.assigned_from <= CURRENT_TIMESTAMP
              AND (sa.assigned_to IS NULL OR sa.assigned_to >= CURRENT_TIMESTAMP)
            ORDER BY t.departure_time ASC
            """, rs -> rs.next() ? Optional.of(mapTrip(rs)) : Optional.empty(), userId);
    }

    public Optional<Map<String,Object>> previousTrip(long tripId) {
        return jdbc.query("""
            SELECT TOP 1 t.trip_id, b.bus_id, b.bus_number, r.route_id,
                   r.source_city, r.destination_city, t.departure_time, t.arrival_time, t.trip_status
            FROM trips currentTrip
            JOIN trips t ON t.route_id=currentTrip.route_id
                AND t.departure_time < currentTrip.departure_time
                AND t.trip_status <> 'CANCELLED'
            JOIN buses b ON b.bus_id=t.bus_id
            JOIN routes r ON r.route_id=t.route_id
            WHERE currentTrip.trip_id=?
            ORDER BY t.departure_time DESC
            """, rs -> rs.next() ? Optional.of(mapPrevious(rs)) : Optional.empty(), tripId);
    }

    public Optional<Map<String,Object>> latestLocation(long tripId) {
        return jdbc.query("""
            SELECT TOP 1 location_id, trip_id, bus_id, latitude, longitude, speed_kmh,
                   heading_degrees, recorded_at
            FROM bus_location_updates WHERE trip_id=?
            ORDER BY recorded_at DESC
            """, rs -> rs.next() ? Optional.of(Map.of(
                    "locationId", rs.getLong("location_id"),
                    "tripId", rs.getLong("trip_id"),
                    "busId", rs.getLong("bus_id"),
                    "latitude", rs.getBigDecimal("latitude"),
                    "longitude", rs.getBigDecimal("longitude"),
                    "speedKmh", rs.getBigDecimal("speed_kmh"),
                    "headingDegrees", rs.getBigDecimal("heading_degrees"),
                    "recordedAt", rs.getTimestamp("recorded_at").toInstant())) : Optional.empty(), tripId);
    }

    public void saveLocation(long userId, long tripId, java.math.BigDecimal lat, java.math.BigDecimal lon,
                             java.math.BigDecimal speed, java.math.BigDecimal heading) {
        Long busId = jdbc.queryForObject("SELECT bus_id FROM trips WHERE trip_id=?", Long.class, tripId);
        jdbc.update("""
            INSERT INTO bus_location_updates
            (trip_id,bus_id,latitude,longitude,speed_kmh,heading_degrees,recorded_at)
            VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)""",
            tripId, busId, lat, lon, speed, heading);
    }

    public void saveMessage(long userId, String subject, String message, boolean urgent) {
        jdbc.update("""
            INSERT INTO operations_messages(sender_user_id, subject, message, urgent, created_at)
            VALUES(?,?,?,?,CURRENT_TIMESTAMP)""", userId, subject.trim(), message.trim(), urgent);
    }

    public List<Map<String, Object>> messages(long userId, boolean admin) {
        String sql = """
            SELECT TOP 100 m.message_id messageId, m.subject, m.message, m.urgent,
                   m.created_at createdAt, u.full_name senderName, u.role senderRole
            FROM operations_messages m
            JOIN users u ON u.user_id=m.sender_user_id
            WHERE (? = 1 OR m.sender_user_id=?)
            ORDER BY m.urgent DESC, m.created_at DESC""";
        return jdbc.queryForList(sql, admin ? 1 : 0, userId);
    }

    public Optional<Map<String,Object>> currentAssignmentForUser(long userId) {
        return assignment(userId);
    }

    private static Map<String,Object> mapTrip(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Map.ofEntries(
            Map.entry("assignmentId", rs.getLong("assignment_id")),
            Map.entry("tripId", rs.getLong("trip_id")),
            Map.entry("role", rs.getString("role")),
            Map.entry("busId", rs.getLong("bus_id")),
            Map.entry("busNumber", rs.getString("bus_number")),
            Map.entry("routeId", rs.getLong("route_id")),
            Map.entry("sourceCity", rs.getString("source_city")),
            Map.entry("destinationCity", rs.getString("destination_city")),
            Map.entry("departureTime", rs.getTimestamp("departure_time").toInstant()),
            Map.entry("arrivalTime", rs.getTimestamp("arrival_time").toInstant()),
            Map.entry("tripStatus", rs.getString("trip_status")),
            Map.entry("baseFare", rs.getBigDecimal("base_fare"))
        );
    }

    private static Map<String,Object> mapPrevious(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Map.ofEntries(
            Map.entry("tripId", rs.getLong("trip_id")),
            Map.entry("busId", rs.getLong("bus_id")),
            Map.entry("busNumber", rs.getString("bus_number")),
            Map.entry("routeId", rs.getLong("route_id")),
            Map.entry("sourceCity", rs.getString("source_city")),
            Map.entry("destinationCity", rs.getString("destination_city")),
            Map.entry("departureTime", rs.getTimestamp("departure_time").toInstant()),
            Map.entry("arrivalTime", rs.getTimestamp("arrival_time").toInstant()),
            Map.entry("tripStatus", rs.getString("trip_status"))
        );
    }
}
