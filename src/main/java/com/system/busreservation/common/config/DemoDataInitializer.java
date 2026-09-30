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

        // Also ensure kandy staff from data-seed.sql have staff profiles
        Long kandyConductor = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE email = 'conductor.kandy@busreservation.lk'");
        if (kandyConductor != null) {
            ensureStaff(kandyConductor, "CON-KANDY-01", "CONDUCTOR-KANDY-QR");
        }
        Long kandyDriver = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE email = 'driver.kandy@busreservation.lk'");
        if (kandyDriver != null) {
            ensureStaff(kandyDriver, "DRV-KANDY-01", "DRIVER-KANDY-QR");
        }

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
}
