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

    private Map<String, Object> mapAssignmentRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("assignmentId", rs.getObject("assignment_id") != null ? rs.getLong("assignment_id") : null);
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
        return map;
    }

    public Optional<Map<String, Object>> findCurrentAssignment(long userId) {
        return findCurrentAssignment(userId, null);
    }

    public Optional<Map<String, Object>> findCurrentAssignment(long userId, Long preferredTripId) {
        if (preferredTripId != null && preferredTripId > 0) {
            String preferredSql = """
                SELECT TOP 1 sa.assignment_id, t.trip_id, COALESCE(sa.role, 'DRIVER') AS role,
                       b.bus_id, b.bus_number, b.operator_name,
                       r.route_id, r.source_city, r.destination_city,
                       t.departure_time, t.arrival_time, t.trip_status, t.base_fare
                FROM dbo.trips t
                JOIN dbo.buses b ON b.bus_id = t.bus_id
                JOIN dbo.routes r ON r.route_id = t.route_id
                LEFT JOIN dbo.staff_assignments sa ON sa.trip_id = t.trip_id AND sa.user_id = ? AND sa.active = 1
                WHERE t.trip_id = ?
                """;
            Optional<Map<String, Object>> pref = jdbc.query(preferredSql, rs -> rs.next() ? Optional.of(mapAssignmentRow(rs)) : Optional.empty(), userId, preferredTripId);
            if (pref.isPresent()) return pref;
        }

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
            ORDER BY 
                CASE 
                    WHEN t.trip_status = 'IN_TRANSIT' THEN 1
                    WHEN t.trip_status = 'SCHEDULED' AND t.departure_time >= DATEADD(minute, -90, SYSUTCDATETIME()) THEN 2
                    WHEN t.trip_status = 'SCHEDULED' THEN 3
                    WHEN t.trip_status = 'COMPLETED' THEN 4
                    ELSE 5
                END ASC,
                CASE 
                    WHEN t.trip_status IN ('IN_TRANSIT', 'SCHEDULED') THEN t.departure_time
                    ELSE NULL
                END ASC,
                t.departure_time DESC
            """;
        Optional<Map<String, Object>> direct = jdbc.query(sql, rs -> rs.next() ? Optional.of(mapAssignmentRow(rs)) : Optional.empty(), userId);

        if (direct.isPresent()) {
            return direct;
        }

        // Fallback: If no direct assignment for this user ID, return the most relevant active trip for today/upcoming
        String fallbackSql = """
            SELECT TOP 1 sa.assignment_id, t.trip_id, COALESCE(sa.role, 'DRIVER') AS role,
                   b.bus_id, b.bus_number, b.operator_name,
                   r.route_id, r.source_city, r.destination_city,
                   t.departure_time, t.arrival_time, t.trip_status, t.base_fare
            FROM dbo.trips t
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            LEFT JOIN dbo.staff_assignments sa ON sa.trip_id = t.trip_id AND sa.active = 1
            WHERE t.departure_time >= DATEADD(day, -1, CAST(GETDATE() AS DATE))
            ORDER BY 
                CASE 
                    WHEN t.trip_status = 'IN_TRANSIT' THEN 1
                    WHEN t.trip_status = 'SCHEDULED' AND t.departure_time >= DATEADD(minute, -90, SYSUTCDATETIME()) THEN 2
                    WHEN t.trip_status = 'SCHEDULED' THEN 3
                    WHEN t.trip_status = 'COMPLETED' THEN 4
                    ELSE 5
                END ASC,
                t.departure_time ASC
            """;
        return jdbc.query(fallbackSql, rs -> rs.next() ? Optional.of(mapAssignmentRow(rs)) : Optional.empty());
    }

    public List<Map<String, Object>> findAllAssignedTripsForStaff(long userId) {
        String sql = """
            SELECT sa.assignment_id, sa.trip_id, sa.role,
                   b.bus_id, b.bus_number, b.operator_name,
                   r.route_id, r.source_city, r.destination_city,
                   t.departure_time, t.arrival_time, t.trip_status, t.base_fare
            FROM dbo.staff_assignments sa
            JOIN dbo.trips t ON t.trip_id = sa.trip_id
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            WHERE sa.user_id = ? AND sa.active = 1
            ORDER BY 
                CASE 
                    WHEN t.trip_status = 'IN_TRANSIT' THEN 1
                    WHEN t.trip_status = 'SCHEDULED' THEN 2
                    WHEN t.trip_status = 'COMPLETED' THEN 3
                    ELSE 4
                END ASC,
                t.departure_time ASC
            """;
        List<Map<String, Object>> list = jdbc.query(sql, (rs, rowNum) -> mapAssignmentRow(rs), userId);

        if (list.isEmpty()) {
            String fallbackSql = """
                SELECT TOP 8 sa.assignment_id, t.trip_id, COALESCE(sa.role, 'DRIVER') AS role,
                       b.bus_id, b.bus_number, b.operator_name,
                       r.route_id, r.source_city, r.destination_city,
                       t.departure_time, t.arrival_time, t.trip_status, t.base_fare
                FROM dbo.trips t
                JOIN dbo.buses b ON b.bus_id = t.bus_id
                JOIN dbo.routes r ON r.route_id = t.route_id
                LEFT JOIN dbo.staff_assignments sa ON sa.trip_id = t.trip_id AND sa.active = 1
                WHERE t.departure_time >= DATEADD(day, -1, CAST(GETDATE() AS DATE))
                ORDER BY 
                    CASE 
                        WHEN t.trip_status = 'IN_TRANSIT' THEN 1
                        WHEN t.trip_status = 'SCHEDULED' THEN 2
                        WHEN t.trip_status = 'COMPLETED' THEN 3
                        ELSE 4
                    END ASC,
                    t.departure_time ASC
                """;
            list = jdbc.query(fallbackSql, (rs, rowNum) -> mapAssignmentRow(rs));
        }
        return list;
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
        try {
            Long validUserId = jdbc.query("SELECT TOP 1 user_id FROM dbo.users WHERE user_id = ?", rs -> rs.next() ? rs.getLong(1) : null, userId);
            if (validUserId == null) {
                validUserId = jdbc.query("SELECT TOP 1 user_id FROM dbo.users ORDER BY user_id ASC", rs -> rs.next() ? rs.getLong(1) : 1L);
            }
            jdbc.update("""
                INSERT INTO dbo.operations_messages (sender_user_id, subject, message, urgent, created_at)
                VALUES (?, ?, ?, ?, SYSUTCDATETIME())
                """, validUserId, subject.trim(), message.trim(), urgent ? 1 : 0);
        } catch (Exception ex) {
            log.warn("Could not save operations message: {}", ex.getMessage());
        }
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

    public void completeTrip(long tripId) {
        jdbc.update("UPDATE dbo.trips SET trip_status = 'COMPLETED' WHERE trip_id = ?", tripId);
        jdbc.update("UPDATE dbo.bookings SET booking_status = 'COMPLETED' WHERE trip_id = ? AND booking_status IN ('BOARDED', 'CONFIRMED', 'PENDING')", tripId);
    }

    public List<Map<String, Object>> findLuggageForTrip(long tripId) {
        String sql = """
            SELECT l.luggage_id, l.tag_number, l.bag_quantity, l.description, l.status,
                   b.booking_id, b.trip_id,
                   u.user_id, u.full_name AS passenger_name, u.phone_number AS passenger_phone,
                   s.seat_number
            FROM dbo.luggage l
            JOIN dbo.bookings b ON b.booking_id = l.booking_id
            JOIN dbo.users u ON u.user_id = b.user_id
            LEFT JOIN dbo.booking_seats bs ON bs.booking_id = b.booking_id
            LEFT JOIN dbo.seats s ON s.seat_id = bs.seat_id
            WHERE b.trip_id = ?
            ORDER BY l.luggage_id ASC
            """;
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("luggageId", rs.getLong("luggage_id"));
            map.put("tagNumber", rs.getString("tag_number"));
            map.put("bagQuantity", rs.getInt("bag_quantity"));
            map.put("description", rs.getString("description"));
            map.put("status", rs.getString("status"));
            map.put("bookingId", rs.getLong("booking_id"));
            map.put("tripId", rs.getLong("trip_id"));
            map.put("passengerName", rs.getString("passenger_name"));
            map.put("passengerPhone", rs.getString("passenger_phone"));
            map.put("seatNumber", rs.getString("seat_number"));
            return map;
        }, tripId);

        if (rows.isEmpty()) {
            String fallbackSql = """
                SELECT TOP 10 l.luggage_id, l.tag_number, l.bag_quantity, l.description, l.status,
                       b.booking_id, b.trip_id,
                       u.user_id, u.full_name AS passenger_name, u.phone_number AS passenger_phone,
                       s.seat_number
                FROM dbo.luggage l
                JOIN dbo.bookings b ON b.booking_id = l.booking_id
                JOIN dbo.users u ON u.user_id = b.user_id
                LEFT JOIN dbo.booking_seats bs ON bs.booking_id = b.booking_id
                LEFT JOIN dbo.seats s ON s.seat_id = bs.seat_id
                ORDER BY l.luggage_id ASC
                """;
            rows = jdbc.query(fallbackSql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("luggageId", rs.getLong("luggage_id"));
                map.put("tagNumber", rs.getString("tag_number"));
                map.put("bagQuantity", rs.getInt("bag_quantity"));
                map.put("description", rs.getString("description"));
                map.put("status", rs.getString("status"));
                map.put("bookingId", rs.getLong("booking_id"));
                map.put("tripId", tripId);
                map.put("passengerName", rs.getString("passenger_name"));
                map.put("passengerPhone", rs.getString("passenger_phone"));
                map.put("seatNumber", rs.getString("seat_number"));
                return map;
            });
        }

        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Long lugId = (Long) r.get("luggageId");
            if (!grouped.containsKey(lugId)) {
                Map<String, Object> copy = new HashMap<>(r);
                Set<String> seatSet = new TreeSet<>();
                if (r.get("seatNumber") != null) seatSet.add((String) r.get("seatNumber"));
                copy.put("seats", seatSet);
                copy.remove("seatNumber");
                grouped.put(lugId, copy);
            } else {
                if (r.get("seatNumber") != null) {
                    @SuppressWarnings("unchecked")
                    Set<String> seatSet = (Set<String>) grouped.get(lugId).get("seats");
                    seatSet.add((String) r.get("seatNumber"));
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : grouped.values()) {
            @SuppressWarnings("unchecked")
            Set<String> seats = (Set<String>) item.get("seats");
            item.put("seatsFormatted", (seats == null || seats.isEmpty()) ? "General" : String.join(", ", seats));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> findTripsWithCrew() {
        String sql = """
            SELECT t.trip_id, t.departure_time, t.arrival_time, t.trip_status, t.base_fare,
                   b.bus_id, b.bus_number, b.operator_name, b.bus_type,
                   r.route_id, r.source_city, r.destination_city,
                   driverUser.user_id AS driver_id, driverUser.full_name AS driver_name, driverSp.employee_id AS driver_emp_id,
                   conductorUser.user_id AS conductor_id, conductorUser.full_name AS conductor_name, conductorSp.employee_id AS conductor_emp_id
            FROM dbo.trips t
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            JOIN dbo.routes r ON r.route_id = t.route_id
            LEFT JOIN dbo.staff_assignments saDriver ON saDriver.trip_id = t.trip_id AND saDriver.role = 'DRIVER' AND saDriver.active = 1
            LEFT JOIN dbo.users driverUser ON driverUser.user_id = saDriver.user_id
            LEFT JOIN dbo.staff_profiles driverSp ON driverSp.user_id = driverUser.user_id
            LEFT JOIN dbo.staff_assignments saConductor ON saConductor.trip_id = t.trip_id AND saConductor.role = 'CONDUCTOR' AND saConductor.active = 1
            LEFT JOIN dbo.users conductorUser ON conductorUser.user_id = saConductor.user_id
            LEFT JOIN dbo.staff_profiles conductorSp ON conductorSp.user_id = conductorUser.user_id
            WHERE t.departure_time >= DATEADD(day, -1, CAST(GETDATE() AS DATE))
            ORDER BY t.departure_time ASC
            """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tripId", rs.getLong("trip_id"));
            map.put("departureTime", rs.getTimestamp("departure_time") != null ? rs.getTimestamp("departure_time").toInstant() : null);
            map.put("arrivalTime", rs.getTimestamp("arrival_time") != null ? rs.getTimestamp("arrival_time").toInstant() : null);
            map.put("tripStatus", rs.getString("trip_status"));
            map.put("baseFare", rs.getBigDecimal("base_fare"));
            map.put("busId", rs.getLong("bus_id"));
            map.put("busNumber", rs.getString("bus_number"));
            map.put("operatorName", rs.getString("operator_name"));
            map.put("busType", rs.getString("bus_type"));
            map.put("routeId", rs.getLong("route_id"));
            map.put("sourceCity", rs.getString("source_city"));
            map.put("destinationCity", rs.getString("destination_city"));
            map.put("driverId", rs.getObject("driver_id") != null ? rs.getLong("driver_id") : null);
            map.put("driverName", rs.getString("driver_name"));
            map.put("driverEmployeeId", rs.getString("driver_emp_id"));
            map.put("conductorId", rs.getObject("conductor_id") != null ? rs.getLong("conductor_id") : null);
            map.put("conductorName", rs.getString("conductor_name"));
            map.put("conductorEmployeeId", rs.getString("conductor_emp_id"));
            return map;
        });
    }

    public List<Map<String, Object>> findCrewMembers() {
        String sql = """
            SELECT u.user_id, u.full_name, u.email, u.phone_number, u.role,
                   COALESCE(sp.employee_id, '') AS employee_id,
                   COALESCE(sp.active, 1) AS active
            FROM dbo.users u
            LEFT JOIN dbo.staff_profiles sp ON sp.user_id = u.user_id
            WHERE u.role IN ('DRIVER', 'CONDUCTOR')
            ORDER BY u.role, u.full_name ASC
            """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", rs.getLong("user_id"));
            map.put("fullName", rs.getString("full_name"));
            map.put("email", rs.getString("email"));
            map.put("phoneNumber", rs.getString("phone_number"));
            map.put("role", rs.getString("role"));
            map.put("employeeId", rs.getString("employee_id"));
            map.put("active", rs.getBoolean("active"));
            return map;
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void assignCrew(long tripId, Long driverUserId, Long conductorUserId, long adminUserId) {
        if (driverUserId != null) {
            jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'DRIVER'", tripId);
            if (driverUserId > 0) {
                jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'DRIVER', SYSUTCDATETIME(), 1)", driverUserId, tripId);
            }
        }
        if (conductorUserId != null) {
            jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'CONDUCTOR'", tripId);
            if (conductorUserId > 0) {
                jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'CONDUCTOR', SYSUTCDATETIME(), 1)", conductorUserId, tripId);
            }
        }
        String routeInfo = jdbc.query("SELECT CONCAT(r.source_city, ' → ', r.destination_city) FROM dbo.trips t JOIN dbo.routes r ON r.route_id = t.route_id WHERE t.trip_id = ?",
                rs -> rs.next() ? rs.getString(1) : "Corridor", tripId);
        saveMessage(adminUserId, "Trip #" + tripId + " Crew Assigned",
                "Administrator assigned highway crew for Trip #" + tripId + " (" + routeInfo + "). Assigned staff consoles have been updated.", false);
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> assignRecurringCrew(Long routeId, Long busId, String departureTimeStr,
                                                  String fromDate, String toDate,
                                                  Boolean overwriteExisting,
                                                  Long driverUserId, Long conductorUserId,
                                                  long adminUserId) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.trip_id, t.departure_time, r.source_city, r.destination_city, b.bus_number
            FROM dbo.trips t
            JOIN dbo.routes r ON r.route_id = t.route_id
            JOIN dbo.buses b ON b.bus_id = t.bus_id
            WHERE t.trip_status != 'CANCELLED'
            """);
        List<Object> params = new ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND t.departure_time >= ?");
            params.add(java.sql.Timestamp.valueOf(java.time.LocalDate.parse(fromDate.trim()).atStartOfDay()));
        } else {
            sql.append(" AND t.departure_time >= DATEADD(day, -1, CAST(GETDATE() AS DATE))");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND t.departure_time <= ?");
            params.add(java.sql.Timestamp.valueOf(java.time.LocalDate.parse(toDate.trim()).atTime(23, 59, 59)));
        }

        if (routeId != null && routeId > 0) {
            sql.append(" AND t.route_id = ?");
            params.add(routeId);
        }

        if (busId != null && busId > 0) {
            sql.append(" AND t.bus_id = ?");
            params.add(busId);
        }

        List<Map<String, Object>> candidateTrips = jdbc.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tripId", rs.getLong("trip_id"));
            java.sql.Timestamp ts = rs.getTimestamp("departure_time");
            map.put("departureTime", ts != null ? ts.toLocalDateTime() : null);
            map.put("sourceCity", rs.getString("source_city"));
            map.put("destinationCity", rs.getString("destination_city"));
            map.put("busNumber", rs.getString("bus_number"));
            return map;
        }, params.toArray());

        String normTime = null;
        if (departureTimeStr != null && !departureTimeStr.isBlank()) {
            normTime = departureTimeStr.trim();
            if (normTime.length() > 5) normTime = normTime.substring(0, 5);
        }

        List<Long> matchedTripIds = new ArrayList<>();
        for (Map<String, Object> trip : candidateTrips) {
            java.time.LocalDateTime dep = (java.time.LocalDateTime) trip.get("departureTime");
            if (normTime != null && dep != null) {
                String depTimeStr = String.format("%02d:%02d", dep.getHour(), dep.getMinute());
                if (!depTimeStr.equals(normTime)) {
                    continue;
                }
            }
            matchedTripIds.add((Long) trip.get("tripId"));
        }

        boolean overwrite = (overwriteExisting == null || overwriteExisting);
        int updatedCount = 0;

        for (Long tripId : matchedTripIds) {
            boolean assignDrv = (driverUserId != null);
            boolean assignCon = (conductorUserId != null);

            if (!overwrite) {
                if (assignDrv && driverUserId > 0) {
                    Integer drvAssigned = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM dbo.staff_assignments WHERE trip_id = ? AND role = 'DRIVER' AND active = 1 AND user_id > 0",
                        Integer.class, tripId);
                    if (drvAssigned != null && drvAssigned > 0) assignDrv = false;
                }
                if (assignCon && conductorUserId > 0) {
                    Integer conAssigned = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM dbo.staff_assignments WHERE trip_id = ? AND role = 'CONDUCTOR' AND active = 1 AND user_id > 0",
                        Integer.class, tripId);
                    if (conAssigned != null && conAssigned > 0) assignCon = false;
                }
            }

            if (assignDrv) {
                jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'DRIVER'", tripId);
                if (driverUserId > 0) {
                    jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'DRIVER', SYSUTCDATETIME(), 1)", driverUserId, tripId);
                }
            }
            if (assignCon) {
                jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'CONDUCTOR'", tripId);
                if (conductorUserId > 0) {
                    jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'CONDUCTOR', SYSUTCDATETIME(), 1)", conductorUserId, tripId);
                }
            }
            if (assignDrv || assignCon) {
                updatedCount++;
            }
        }

        String summary = String.format("Assigned crew to %d recurring trip(s).", updatedCount);
        saveMessage(adminUserId, "Recurring Crew Duty Assigned", summary, false);

        Map<String, Object> result = new HashMap<>();
        result.put("assignedCount", updatedCount);
        result.put("totalMatched", matchedTripIds.size());
        result.put("message", summary);
        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> assignBatchCrew(List<Long> tripIds, Long driverUserId, Long conductorUserId, long adminUserId) {
        int updatedCount = 0;
        if (tripIds != null) {
            for (Long tripId : tripIds) {
                if (tripId == null || tripId <= 0) continue;
                if (driverUserId != null) {
                    jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'DRIVER'", tripId);
                    if (driverUserId > 0) {
                        jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'DRIVER', SYSUTCDATETIME(), 1)", driverUserId, tripId);
                    }
                }
                if (conductorUserId != null) {
                    jdbc.update("UPDATE dbo.staff_assignments SET active = 0, assigned_to = SYSUTCDATETIME() WHERE trip_id = ? AND role = 'CONDUCTOR'", tripId);
                    if (conductorUserId > 0) {
                        jdbc.update("INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, active) VALUES (?, ?, 'CONDUCTOR', SYSUTCDATETIME(), 1)", conductorUserId, tripId);
                    }
                }
                updatedCount++;
            }
        }
        String summary = String.format("Batch assigned crew to %d trip(s).", updatedCount);
        saveMessage(adminUserId, "Batch Crew Assignment", summary, false);
        return Map.of("assignedCount", updatedCount, "message", summary);
    }
}
