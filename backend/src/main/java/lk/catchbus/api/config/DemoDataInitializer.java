package lk.catchbus.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DemoDataInitializer(JdbcTemplate jdbc, PasswordEncoder encoder,
                               @Value("${app.demo-seed:true}") boolean enabled) {
        this.jdbc = jdbc; this.encoder = encoder; this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        try {
            Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME IN ('staff_profiles','staff_assignments','bus_location_updates','operations_messages')""", Integer.class);
            if (tableCount == null || tableCount < 4) {
                System.out.println("CatchBusLK: run database/module_schema.sql before starting demo seed.");
                return;
            }
            seedUsersAndStaff();
        } catch (Exception e) {
            System.out.println("CatchBusLK demo seed skipped: " + e.getMessage());
        }
    }

    private void seedUsersAndStaff() {
        long admin = ensureUser("System Admin", "admin@catchbus.lk", "0710000000", "Admin@123", "ADMIN");
        long conductor = ensureUser("Demo Conductor", "conductor@catchbus.lk", "0710000001", "Conductor@123", "CONDUCTOR");
        long driver = ensureUser("Demo Driver", "driver@catchbus.lk", "0710000002", "Driver@123", "DRIVER");

        ensureStaff(conductor, "CON-001", "CONDUCTOR-DEMO-QR");
        ensureStaff(driver, "DRV-001", "DRIVER-DEMO-QR");

        Long route = oneLong("SELECT TOP 1 route_id FROM routes ORDER BY route_id");
        if (route == null) {
            jdbc.update("INSERT INTO routes(source_city,destination_city,estimated_duration_hours) VALUES('Colombo','Negombo',1.50)");
            route = oneLong("SELECT TOP 1 route_id FROM routes ORDER BY route_id DESC");
        }
        Long bus1 = oneLong("SELECT TOP 1 bus_id FROM buses WHERE bus_number='CBL-001'");
        if (bus1 == null) {
            jdbc.update("INSERT INTO buses(bus_number,bus_type,total_seats,operator_name) VALUES('CBL-001','AC',52,'CatchBusLK')");
            bus1 = oneLong("SELECT bus_id FROM buses WHERE bus_number='CBL-001'");
        }
        Long bus2 = oneLong("SELECT TOP 1 bus_id FROM buses WHERE bus_number='CBL-000'");
        if (bus2 == null) {
            jdbc.update("INSERT INTO buses(bus_number,bus_type,total_seats,operator_name) VALUES('CBL-000','AC',52,'CatchBusLK')");
            bus2 = oneLong("SELECT bus_id FROM buses WHERE bus_number='CBL-000'");
        }

        LocalDateTime now = LocalDateTime.now();
        Long previousTrip = oneLong("SELECT TOP 1 trip_id FROM trips WHERE bus_id=? AND route_id=? ORDER BY departure_time DESC", bus2, route);
        if (previousTrip == null) {
            jdbc.update("INSERT INTO trips(bus_id,route_id,departure_time,arrival_time,base_fare,trip_status) VALUES(?,?,?,?,?,'IN_TRANSIT')",
                    bus2, route, now.minusHours(1), now.plusMinutes(30), 250.00);
            previousTrip = oneLong("SELECT TOP 1 trip_id FROM trips WHERE bus_id=? AND route_id=? ORDER BY trip_id DESC", bus2, route);
        }
        Long currentTrip = oneLong("SELECT TOP 1 trip_id FROM trips WHERE bus_id=? AND route_id=? ORDER BY departure_time DESC", bus1, route);
        if (currentTrip == null) {
            jdbc.update("INSERT INTO trips(bus_id,route_id,departure_time,arrival_time,base_fare,trip_status) VALUES(?,?,?,?,?,'SCHEDULED')",
                    bus1, route, now.plusMinutes(30), now.plusHours(2), 250.00);
            currentTrip = oneLong("SELECT TOP 1 trip_id FROM trips WHERE bus_id=? AND route_id=? ORDER BY trip_id DESC", bus1, route);
        }

        assignIfMissing(conductor, currentTrip, "CONDUCTOR");
        assignIfMissing(driver, currentTrip, "DRIVER");

        Integer loc = jdbc.queryForObject("SELECT COUNT(*) FROM bus_location_updates WHERE trip_id=?", Integer.class, previousTrip);
        if (loc != null && loc == 0) {
            jdbc.update("""
                INSERT INTO bus_location_updates
                (trip_id,bus_id,latitude,longitude,speed_kmh,heading_degrees,recorded_at)
                VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)""",
                previousTrip, bus2, 7.2088, 79.8358, 42.0, 180.0);
        }
    }

    private long ensureUser(String name, String email, String phone, String password, String role) {
        Long id = oneLong("SELECT TOP 1 user_id FROM users WHERE email=?", email);
        if (id != null) return id;
        jdbc.update("""
                       INSERT INTO users(full_name,email,phone_number,password_hash,role,created_at)
                       VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)""",
                name,email,phone,encoder.encode(password),role);
        return oneLong("SELECT TOP 1 user_id FROM users WHERE email=?", email);
    }

    private void ensureStaff(long userId, String employeeId, String qr) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM staff_profiles WHERE user_id=?", Integer.class, userId);
        if (n == null || n == 0) {
            jdbc.update("INSERT INTO staff_profiles(user_id,employee_id,qr_token,active) VALUES(?,?,?,1)",
                    userId, employeeId, qr);
        }
    }

    private void assignIfMissing(long userId, long tripId, String role) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignments WHERE user_id=? AND trip_id=? AND active=1",
                Integer.class, userId, tripId);
        if (n == null || n == 0) {
            jdbc.update("""
                           INSERT INTO staff_assignments(user_id,trip_id,role,assigned_from,assigned_to,active)
                           VALUES(?,?,?,CURRENT_TIMESTAMP,NULL,1)""", userId, tripId, role);
        }
    }

    private Long oneLong(String sql, Object... args) {
        var list = jdbc.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
        return list;
    }
}
