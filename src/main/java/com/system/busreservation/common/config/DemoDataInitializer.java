package com.system.busreservation.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Initializes CatchBusLK demo staff accounts, QR tokens, and live assignments on startup
 * if they are not already present in BusReservationDB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        try {
            Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME IN ('staff_profiles', 'staff_assignments', 'bus_location_updates', 'operations_messages')
                """, Integer.class);

            if (tableCount == null || tableCount < 4) {
                log.info("DemoDataInitializer: Module schema tables not yet detected, skipping demo seed.");
                return;
            }

            seedDemoStaffAndTrips();
        } catch (Exception e) {
            log.warn("DemoDataInitializer note: {}", e.getMessage());
        }
    }

    private void seedDemoStaffAndTrips() {
        long admin = ensureUser("CatchBus Admin", "admin@catchbus.lk", "0710000000", "Admin@123", "ADMIN");
        ensureUser("System Administrator", "admin@busreservation.lk", "+94771234567", "Admin@123", "ADMIN");
        long conductor = ensureUser("Demo Conductor", "conductor@catchbus.lk", "0710000001", "Conductor@123", "CONDUCTOR");
        long driver = ensureUser("Demo Driver", "driver@catchbus.lk", "0710000002", "Driver@123", "DRIVER");

        // Also ensure kandy staff from data-seed.sql have valid passwords and staff profiles
        long kandyConductor = ensureUser("Kandy Demo Conductor", "conductor.kandy@busreservation.lk", "+94771234568", "Password@123", "CONDUCTOR");
        ensureStaff(kandyConductor, "CON-KANDY-01", "CONDUCTOR-KANDY-QR");

        long kandyDriver = ensureUser("Kandy Demo Driver", "driver.kandy@busreservation.lk", "+94771234569", "Password@123", "DRIVER");
        ensureStaff(kandyDriver, "DRV-KANDY-01", "DRIVER-KANDY-QR");

        ensureStaff(conductor, "CON-001", "CONDUCTOR-DEMO-QR");
        ensureStaff(driver, "DRV-001", "DRIVER-DEMO-QR");

        Long route = oneLong("SELECT TOP 1 route_id FROM dbo.routes ORDER BY route_id");
        if (route == null) {
            jdbc.update("INSERT INTO dbo.routes(source_city, destination_city, estimated_duration_hours) VALUES ('Colombo', 'Kandy', 3.25)");
            route = oneLong("SELECT TOP 1 route_id FROM dbo.routes ORDER BY route_id DESC");
        }

        Long bus1 = oneLong("SELECT TOP 1 bus_id FROM dbo.buses WHERE bus_number = 'CBL-001'");
        if (bus1 == null) {
            jdbc.update("INSERT INTO dbo.buses(bus_number, bus_type, total_seats, operator_name) VALUES ('CBL-001', 'AC', 52, 'CatchBusLK Luxury')");
            bus1 = oneLong("SELECT bus_id FROM dbo.buses WHERE bus_number = 'CBL-001'");
        }

        Long bus2 = oneLong("SELECT TOP 1 bus_id FROM dbo.buses WHERE bus_number = 'CBL-000'");
        if (bus2 == null) {
            jdbc.update("INSERT INTO dbo.buses(bus_number, bus_type, total_seats, operator_name) VALUES ('CBL-000', 'AC', 52, 'CatchBusLK Express')");
            bus2 = oneLong("SELECT bus_id FROM dbo.buses WHERE bus_number = 'CBL-000'");
        }

        LocalDateTime now = LocalDateTime.now();
        Long previousTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY departure_time DESC", bus2, route);
        if (previousTrip == null) {
            jdbc.update("INSERT INTO dbo.trips(bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, 850.00, 'IN_TRANSIT')",
                    bus2, route, now.minusHours(1), now.plusMinutes(45));
            previousTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY trip_id DESC", bus2, route);
        }

        Long currentTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY departure_time DESC", bus1, route);
        if (currentTrip == null) {
            jdbc.update("INSERT INTO dbo.trips(bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, 850.00, 'SCHEDULED')",
                    bus1, route, now.plusMinutes(30), now.plusHours(3));
            currentTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY trip_id DESC", bus1, route);
        }

        assignIfMissing(conductor, currentTrip, "CONDUCTOR");
        assignIfMissing(driver, currentTrip, "DRIVER");

        Integer locCount = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bus_location_updates WHERE trip_id = ?", Integer.class, previousTrip);
        if (locCount != null && locCount == 0) {
            jdbc.update("""
                INSERT INTO dbo.bus_location_updates (trip_id, bus_id, latitude, longitude, speed_kmh, heading_degrees, recorded_at)
                VALUES (?, ?, 7.2906, 80.6337, 48.5, 75.0, SYSUTCDATETIME())
                """, previousTrip, bus2);
        }

        seedDemoNotifications();

        log.info("DemoDataInitializer: Verified CatchBusLK demo staff accounts and trip assignments.");
    }

    private long ensureUser(String name, String email, String phone, String password, String role) {
        Long id = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE LOWER(email) = LOWER(?)", email);
        String hashed = encoder.encode(password);
        if (id != null) {
            jdbc.update("UPDATE dbo.users SET password_hash = ?, role = ? WHERE user_id = ?", hashed, role, id);
            return id;
        }
        jdbc.update("""
            INSERT INTO dbo.users(full_name, email, phone_number, password_hash, role, created_at)
            VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())
            """, name, email, phone, hashed, role);
        return oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE LOWER(email) = LOWER(?)", email);
    }

    private void ensureStaff(long userId, String employeeId, String qrToken) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.staff_profiles WHERE user_id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            jdbc.update("""
                INSERT INTO dbo.staff_profiles(user_id, employee_id, qr_token, active)
                VALUES (?, ?, ?, 1)
                """, userId, employeeId, qrToken);
        }
    }

    private void assignIfMissing(long userId, long tripId, String role) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM dbo.staff_assignments WHERE user_id = ? AND trip_id = ? AND active = 1
            """, Integer.class, userId, tripId);
        if (count == null || count == 0) {
            jdbc.update("""
                INSERT INTO dbo.staff_assignments(user_id, trip_id, role, assigned_from, assigned_to, active)
                VALUES (?, ?, ?, SYSUTCDATETIME(), NULL, 1)
                """, userId, tripId, role);
        }
    }

    private Long oneLong(String sql, Object... args) {
        return jdbc.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
    }

    private void seedDemoNotifications() {
        try {
            Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'notifications'
                """, Integer.class);
            if (tableCount == null || tableCount == 0) return;

            Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.notifications WHERE type IN ('BOOKING_CONFIRMED', 'JOURNEY_REMINDER', 'ROUTE_DELAY')
                """, Integer.class);
            if (count != null && count > 0) return;

            Long adminId = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE LOWER(email) = 'admin@catchbus.lk'");
            if (adminId == null) adminId = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE role = 'ADMIN'");
            Long passengerId = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE role = 'PASSENGER' ORDER BY user_id ASC");
            if (passengerId == null) passengerId = adminId;

            Long booking = oneLong("SELECT TOP 1 booking_id FROM dbo.bookings ORDER BY booking_id DESC");

            LocalDateTime now = LocalDateTime.now();

            insertNotification(passengerId, booking, "Booking Confirmed: E-Ticket #CBL-8842 issued for Colombo to Kandy (Express Luxury AC). Departure today at 14:30 from Platform Bay 03. QR boarding pass ready.", "BOOKING_CONFIRMED", now.minusMinutes(15), false);
            insertNotification(passengerId, booking, "Luggage Tag Registered: Tag #LUG-20261002-882163 (1 standard bag, 12.5 kg) verified and assigned to Bay B security hold. QR Baggage Tag synced.", "LUGGAGE_TAGGED", now.minusMinutes(35), false);
            insertNotification(passengerId, booking, "Departure Reminder: Bus CBL-001 departs in 45 minutes from Bastian Mawatha Terminal Bay 3. Conductor check-in is now open.", "JOURNEY_REMINDER", now.minusMinutes(55), false);
            insertNotification(adminId != null ? adminId : passengerId, null, "Traffic & Road Advisory: Road resurfacing near Dodangoda on Southern Expressway (E01). Minor 12-minute transit delay expected.", "ROUTE_DELAY", now.minusHours(2), true);
            insertNotification(passengerId, booking, "Luggage Status Update: Bag Tag #LUG-20261001-527512 status updated to CHECKED_IN by Galle Conductor.", "LUGGAGE_STATUS", now.minusHours(3), true);
            insertNotification(passengerId, booking, "Boarding Gate Assignment: Northern Superline (Colombo to Jaffna) will board from Terminal 2, Platform Bay 05.", "GATE_UPDATE", now.minusHours(4), true);
            insertNotification(passengerId, null, "Customer Care Resolution: Support response received regarding your recent inquiry — 'Our fleet team has verified the air-conditioning unit on bus CBL-001. Thank you for your feedback!'", "FEEDBACK_REPLY", now.minusHours(6), true);
            insertNotification(passengerId, booking, "Booking Confirmed: E-Ticket #CBL-9102 issued for Colombo to Badulla (Ella Superline). Reserved Seat: 14A. Safe travels!", "BOOKING_CONFIRMED", now.minusHours(8), true);

            log.info("DemoDataInitializer: Seeded demo platform and passenger notifications.");
        } catch (Exception e) {
            log.warn("DemoDataInitializer seed notifications note: {}", e.getMessage());
        }
    }

    private void insertNotification(Long userId, Long bookingId, String message, String type, LocalDateTime createdAt, boolean isRead) {
        if (userId == null) return;
        jdbc.update("""
            INSERT INTO dbo.notifications (user_id, booking_id, message, type, created_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?)
            """, userId, bookingId, message, type, createdAt, isRead ? 1 : 0);
    }
}
