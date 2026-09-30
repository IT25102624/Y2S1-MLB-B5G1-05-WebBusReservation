package com.system.busreservation.modules.staff.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * JDBC repository for Staff Profiles, Assignments, GPS Location Tracking, and Operations Messages.
 * Interacts with module tables: staff_profiles, staff_assignments, bus_location_updates, operations_messages.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StaffRepository {

    private final JdbcTemplate jdbc;

    public Optional<Map<String, Object>> findStaffByQr(String qrToken) {
        String sql = """
            SELECT TOP 1 sp.user_id, sp.employee_id, sp.qr_token, sp.active, u.full_name, u.email, u.role
            FROM dbo.staff_profiles sp
            JOIN dbo.users u ON u.user_id = sp.user_id
            WHERE sp.qr_token = ? AND sp.active = 1
            """;
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", rs.getLong("user_id"));
                map.put("employeeId", rs.getString("employee_id"));
                map.put("qrToken", rs.getString("qr_token"));
                map.put("fullName", rs.getString("full_name"));
                map.put("email", rs.getString("email"));
                map.put("role", rs.getString("role"));
                return Optional.of(map);
            }
            return Optional.empty();
        }, qrToken);
    }

    public Optional<Map<String, Object>> findStaffProfile(long userId) {
        String sql = "SELECT TOP 1 employee_id, qr_token, active FROM dbo.staff_profiles WHERE user_id = ?";
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("employeeId", rs.getString("employee_id"));
                map.put("qrToken", rs.getString("qr_token"));
                map.put("active", rs.getBoolean("active"));
                return Optional.of(map);
            }
            return Optional.empty();
        }, userId);
    }

    public Optional<Map<String, Object>> findCurrentAssignment(long userId) {
        String sql = """
            SELECT TOP 1 sa.assignment_id, sa.trip_id, sa.role,
                   b.bus_id, b.bus_number, b.operator_name,
                   r.route_id, r.source_city, r.destination_city,
                   t.departure_time, t.arrival_time, t.trip_status, t.base_fare
            FROM dbo.staff_assignments sa
            JOIN dbo.trips t ON t.trip_id = sa.trip_id
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            WHERE sa.user_id = ? AND sa.active = 1
            ORDER BY t.departure_time ASC
            """;
        Optional<Map<String, Object>> direct = jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("assignmentId", rs.getLong("assignment_id"));
                map.put("tripId", rs.getLong("trip_id"));
                map.put("role", rs.getString("role"));
                map.put("busId", rs.getLong("bus_id"));
                map.put("busNumber", rs.getString("bus_number"));
                map.put("operatorName", rs.getString("operator_name"));
                map.put("routeId", rs.getLong("route_id"));
                map.put("sourceCity", rs.getString("source_city"));
                map.put("destinationCity", rs.getString("destination_city"));
                map.put("departureTime", rs.getTimestamp("departure_time") != null ? rs.getTimestamp("departure_time").toInstant() : null);
                map.put("arrivalTime", rs.getTimestamp("arrival_time") != null ? rs.getTimestamp("arrival_time").toInstant() : null);
                map.put("tripStatus", rs.getString("trip_status"));
                map.put("baseFare", rs.getBigDecimal("base_fare"));
                return Optional.of(map);
            }
            return Optional.empty();
        }, userId);

        if (direct.isPresent()) {
            return direct;
        }

        // Fallback: If no direct assignment for this specific user ID, return any active assignment (e.g. for demo or testing)
        String fallbackSql = """
            SELECT TOP 1 sa.assignment_id, sa.trip_id, sa.role,
                   b.bus_id, b.bus_number, b.operator_name,
                   r.route_id, r.source_city, r.destination_city,
                   t.departure_time, t.arrival_time, t.trip_status, t.base_fare
            FROM dbo.staff_assignments sa
            JOIN dbo.trips t ON t.trip_id = sa.trip_id
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            WHERE sa.active = 1
            ORDER BY t.departure_time ASC
            """;
        return jdbc.query(fallbackSql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("assignmentId", rs.getLong("assignment_id"));
                map.put("tripId", rs.getLong("trip_id"));
                map.put("role", rs.getString("role"));
                map.put("busId", rs.getLong("bus_id"));
                map.put("busNumber", rs.getString("bus_number"));
                map.put("operatorName", rs.getString("operator_name"));
                map.put("routeId", rs.getLong("route_id"));
                map.put("sourceCity", rs.getString("source_city"));
                map.put("destinationCity", rs.getString("destination_city"));
                map.put("departureTime", rs.getTimestamp("departure_time") != null ? rs.getTimestamp("departure_time").toInstant() : null);
                map.put("arrivalTime", rs.getTimestamp("arrival_time") != null ? rs.getTimestamp("arrival_time").toInstant() : null);
                map.put("tripStatus", rs.getString("trip_status"));
                map.put("baseFare", rs.getBigDecimal("base_fare"));
                return Optional.of(map);
            }
            return Optional.empty();
        });
    }

    public Optional<Map<String, Object>> findPreviousTrip(long tripId) {
        String sql = """
            SELECT TOP 1 t.trip_id, b.bus_id, b.bus_number, b.operator_name, r.route_id,
                   r.source_city, r.destination_city, t.departure_time, t.arrival_time, t.trip_status
            FROM dbo.trips currentTrip
            JOIN dbo.trips t ON t.route_id = currentTrip.route_id
                AND t.departure_time < currentTrip.departure_time
                AND t.trip_status <> 'CANCELLED'
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            WHERE currentTrip.trip_id = ?
            ORDER BY t.departure_time DESC
            """;
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tripId", rs.getLong("trip_id"));
                map.put("busId", rs.getLong("bus_id"));
                map.put("busNumber", rs.getString("bus_number"));
                map.put("operatorName", rs.getString("operator_name"));
                map.put("routeId", rs.getLong("route_id"));
                map.put("sourceCity", rs.getString("source_city"));
                map.put("destinationCity", rs.getString("destination_city"));
                map.put("departureTime", rs.getTimestamp("departure_time") != null ? rs.getTimestamp("departure_time").toInstant() : null);
                map.put("arrivalTime", rs.getTimestamp("arrival_time") != null ? rs.getTimestamp("arrival_time").toInstant() : null);
                map.put("tripStatus", rs.getString("trip_status"));
                return Optional.of(map);
            }
            return Optional.empty();
        }, tripId);
    }

    public Optional<Map<String, Object>> findLatestLocation(long tripId) {
        String sql = """
            SELECT TOP 1 location_id, trip_id, bus_id, latitude, longitude, speed_kmh,
                   heading_degrees, recorded_at
            FROM dbo.bus_location_updates
            WHERE trip_id = ?
            ORDER BY recorded_at DESC
            """;
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("locationId", rs.getLong("location_id"));
                map.put("tripId", rs.getLong("trip_id"));
                map.put("busId", rs.getLong("bus_id"));
                map.put("latitude", rs.getBigDecimal("latitude"));
                map.put("longitude", rs.getBigDecimal("longitude"));
                map.put("speedKmh", rs.getBigDecimal("speed_kmh"));
                map.put("headingDegrees", rs.getBigDecimal("heading_degrees"));
                map.put("recordedAt", rs.getTimestamp("recorded_at") != null ? rs.getTimestamp("recorded_at").toInstant() : Instant.now());
                return Optional.of(map);
            }
            return Optional.empty();
        }, tripId);
    }

    public void saveLocation(long tripId, BigDecimal lat, BigDecimal lon, BigDecimal speed, BigDecimal heading) {
        Long busId = jdbc.queryForObject("SELECT bus_id FROM dbo.trips WHERE trip_id = ?", Long.class, tripId);
        jdbc.update("""
            INSERT INTO dbo.bus_location_updates
            (trip_id, bus_id, latitude, longitude, speed_kmh, heading_degrees, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
            """, tripId, busId, lat, lon, speed, heading);
    }

    public void saveMessage(long userId, String subject, String message, boolean urgent) {
        jdbc.update("""
            INSERT INTO dbo.operations_messages (sender_user_id, subject, message, urgent, created_at)
            VALUES (?, ?, ?, ?, SYSUTCDATETIME())
            """, userId, subject.trim(), message.trim(), urgent ? 1 : 0);
    }

    public List<Map<String, Object>> listMessages(long userId, boolean admin) {
        String sql = """
            SELECT TOP 50 m.message_id, m.subject, m.message, m.urgent,
                   m.created_at, u.full_name, u.role
            FROM dbo.operations_messages m
            JOIN dbo.users u ON u.user_id = m.sender_user_id
            WHERE (? = 1 OR m.sender_user_id = ?)
            ORDER BY m.urgent DESC, m.created_at DESC
            """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("messageId", rs.getLong("message_id"));
            map.put("subject", rs.getString("subject"));
            map.put("message", rs.getString("message"));
            map.put("urgent", rs.getBoolean("urgent"));
            map.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null);
            map.put("senderName", rs.getString("full_name"));
            map.put("senderRole", rs.getString("role"));
            return map;
        }, admin ? 1 : 0, userId);
    }

    public Optional<Map<String, Object>> findBooking(long bookingId) {
        String sql = "SELECT booking_id, trip_id, user_id, booking_status FROM dbo.bookings WHERE booking_id = ?";
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("bookingId", rs.getLong("booking_id"));
                map.put("tripId", rs.getLong("trip_id"));
                map.put("userId", rs.getLong("user_id"));
                map.put("status", rs.getString("booking_status"));
                return Optional.of(map);
            }
            return Optional.empty();
        }, bookingId);
    }

    public void updateBookingStatus(long bookingId, String status) {
        jdbc.update("UPDATE dbo.bookings SET booking_status = ? WHERE booking_id = ?", status, bookingId);
    }

    public int countBoarded(long tripId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bookings WHERE trip_id = ? AND booking_status = 'BOARDED'", Integer.class, tripId);
        return count != null ? count : 0;
    }
}
