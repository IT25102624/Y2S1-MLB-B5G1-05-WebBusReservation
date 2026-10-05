package com.system.busreservation.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
        // 1. Admins (password: admin@123)
        ensureUser("System Administrator", "admin@busreservation.lk", "+94771234567", "admin@123", "ADMIN");
        ensureUser("CatchBus Admin", "admin@catchbus.lk", "0710000000", "admin@123", "ADMIN");

        // 2. Conductors (passwords: kasun@123, sunil@123, ruwan@123)
        long conductor1 = ensureUser("Kasun Perera", "conductor.kandy@busreservation.lk", "+94762345678", "kasun@123", "CONDUCTOR");
        ensureStaff(conductor1, "CON-KANDY-01", "CONDUCTOR-KANDY-QR");

        long conductor2 = ensureUser("Sunil Shantha", "conductor@catchbus.lk", "0710000001", "sunil@123", "CONDUCTOR");
        ensureStaff(conductor2, "CON-001", "CONDUCTOR-DEMO-QR");

        long conductor3 = ensureUser("Ruwan Wijesinghe", "ruwan.conductor@catchbus.lk", "+94772233112", "ruwan@123", "CONDUCTOR");
        ensureStaff(conductor3, "CON-COL-02", "CONDUCTOR-COL-QR");

        // 3. Drivers (passwords: nimal@123, chaminda@123, suresh@123, priyantha@123)
        long driver1 = ensureUser("Nimal Silva", "driver.kandy@busreservation.lk", "+94763456789", "nimal@123", "DRIVER");
        ensureStaff(driver1, "DRV-KANDY-01", "DRIVER-KANDY-QR");

        long driver2 = ensureUser("Chaminda Vaas", "driver@catchbus.lk", "0710000002", "chaminda@123", "DRIVER");
        ensureStaff(driver2, "DRV-001", "DRIVER-DEMO-QR");

        long driver3 = ensureUser("Suresh Kumara", "suresh.driver@catchbus.lk", "+94773344221", "suresh@123", "DRIVER");
        ensureStaff(driver3, "DRV-GALLE-02", "DRIVER-GALLE-QR");

        long driver4 = ensureUser("Priyantha Mendis", "priyantha.driver@catchbus.lk", "+94775566332", "priyantha@123", "DRIVER");
        ensureStaff(driver4, "DRV-JAFFNA-03", "DRIVER-JAFFNA-QR");

        // Migrate legacy demo account if present
        try {
            jdbc.update("UPDATE dbo.users SET full_name = 'Kavindu Perera', email = 'kavindu@busreservation.lk' WHERE LOWER(email) = 'dohan@busreservation.lk'");
            jdbc.update("UPDATE dbo.tickets SET passenger_name = 'Kavindu Perera', passenger_email = 'kavindu@busreservation.lk' WHERE LOWER(passenger_email) = 'dohan@busreservation.lk'");
        } catch (Exception ignored) {}

        // 4. Commuter Passengers (passwords: kavindu@123, kamal@123, tharushi@123, dilshan@123, anura@123)
        long kavindu = ensureUser("Kavindu Perera", "kavindu@busreservation.lk", "+94719876543", "kavindu@123", "PASSENGER");
        long kamal = ensureUser("Kamal Gunaratne", "kamal@busreservation.lk", "+94771122334", "kamal@123", "PASSENGER");
        long tharushi = ensureUser("Tharushi Bandara", "tharushi@busreservation.lk", "+94712233445", "tharushi@123", "PASSENGER");
        long dilshan = ensureUser("Dilshan Silva", "dilshan@busreservation.lk", "+94783344556", "dilshan@123", "PASSENGER");
        long anura = ensureUser("Anura Fernando", "anura@busreservation.lk", "+94704455667", "anura@123", "PASSENGER");

        // Format and synchronize all account passwords to firstName@123
        synchronizeUserPasswords();

        long conductor = conductor2;
        long driver = driver2;
        long kandyConductor = conductor1;
        long kandyDriver = driver1;

        Long route = oneLong("SELECT TOP 1 route_id FROM dbo.routes ORDER BY route_id");
        if (route == null) {
            jdbc.update("INSERT INTO dbo.routes(source_city, destination_city, estimated_duration_hours) VALUES ('Colombo', 'Kandy', 3.25)");
            route = oneLong("SELECT TOP 1 route_id FROM dbo.routes ORDER BY route_id DESC");
        }

        // Seed diverse fleet covering all bus types (AC, LUXURY, SLEEPER, NON_AC)
        Long bus1 = ensureBus("CBL-001", "AC", 52, "CatchBusLK Luxury");
        Long bus2 = ensureBus("CBL-000", "AC", 52, "CatchBusLK Express");
        Long busND1001 = ensureBus("ND-1001", "AC", 40, "Superline Express");
        Long busND2002 = ensureBus("ND-2002", "LUXURY", 44, "Rajarata Travels");
        Long busWP3003 = ensureBus("WP-3003", "SLEEPER", 36, "Northern Highway Liner");
        Long busSP4004 = ensureBus("SP-4004", "NON_AC", 50, "Southern Express");
        Long busCP5005 = ensureBus("CP-5005", "AC", 42, "Hill Country Express");
        Long busNE6006 = ensureBus("NE-6006", "LUXURY", 38, "Highland Super Coach");
        Long busEP7007 = ensureBus("EP-7007", "SLEEPER", 32, "Eastern Night Liner");
        Long busNW8008 = ensureBus("NW-8008", "NON_AC", 54, "Wayamba Commuter Liner");

        LocalDateTime now = LocalDateTime.now();
        Long previousTrip = oneLong("""
            SELECT TOP 1 trip_id FROM dbo.trips 
            WHERE bus_id = ? AND route_id = ? AND departure_time >= DATEADD(hour, -4, SYSUTCDATETIME())
            ORDER BY departure_time DESC""", bus2, route);
        if (previousTrip == null) {
            jdbc.update("INSERT INTO dbo.trips(bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, 850.00, 'IN_TRANSIT')",
                    bus2, route, now.minusHours(1), now.plusMinutes(45));
            previousTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY trip_id DESC", bus2, route);
        }

        Long currentTrip = oneLong("""
            SELECT TOP 1 trip_id FROM dbo.trips 
            WHERE bus_id = ? AND route_id = ? AND departure_time >= DATEADD(minute, -30, SYSUTCDATETIME()) AND trip_status = 'SCHEDULED'
            ORDER BY departure_time ASC""", bus1, route);
        if (currentTrip == null) {
            jdbc.update("INSERT INTO dbo.trips(bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, 1250.00, 'SCHEDULED')",
                    bus1, route, now.plusMinutes(35), now.plusHours(3).plusMinutes(35));
            currentTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY trip_id DESC", bus1, route);
        }

        Long secondTrip = oneLong("""
            SELECT TOP 1 trip_id FROM dbo.trips 
            WHERE bus_id = ? AND route_id = ? AND departure_time >= DATEADD(hour, 3, SYSUTCDATETIME()) AND trip_id <> ?
            ORDER BY departure_time ASC""", bus1, route, currentTrip);
        if (secondTrip == null) {
            jdbc.update("INSERT INTO dbo.trips(bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, 1250.00, 'SCHEDULED')",
                    bus1, route, now.plusHours(5), now.plusHours(8).plusMinutes(15));
            secondTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE bus_id = ? AND route_id = ? ORDER BY trip_id DESC", bus1, route);
        }

        assignIfMissing(conductor, currentTrip, "CONDUCTOR");
        assignIfMissing(driver, currentTrip, "DRIVER");
        if (secondTrip != null) {
            assignIfMissing(conductor, secondTrip, "CONDUCTOR");
            assignIfMissing(driver, secondTrip, "DRIVER");
            assignIfMissing(kandyConductor, secondTrip, "CONDUCTOR");
            assignIfMissing(kandyDriver, secondTrip, "DRIVER");
        }

        cleanAndSeedRealisticBookings(kavindu, kamal, tharushi, dilshan, currentTrip, secondTrip, bus1);

        Integer locCount = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bus_location_updates WHERE trip_id = ?", Integer.class, previousTrip);
        if (locCount != null && locCount == 0) {
            jdbc.update("""
                INSERT INTO dbo.bus_location_updates (trip_id, bus_id, latitude, longitude, speed_kmh, heading_degrees, recorded_at)
                VALUES (?, ?, 7.2906, 80.6337, 48.5, 75.0, SYSUTCDATETIME())
                """, previousTrip, bus2);
        }

        seedDemoNotifications();
        seedIntercityRoutesAndTrips(bus1, bus2, busND1001, busND2002, busWP3003, busSP4004, busCP5005, busNE6006, busEP7007, busNW8008);

        log.info("DemoDataInitializer: Verified CatchBusLK demo staff accounts, intercity routes, and trip assignments.");
    }

    private void seedIntercityRoutesAndTrips(Long bus1, Long bus2, Long busND1001, Long busND2002, Long busWP3003, Long busSP4004, Long busCP5005, Long busNE6006, Long busEP7007, Long busNW8008) {
        try {
            // Colombo <-> Kandy (3.25 hrs, varied types: NON_AC, AC, LUXURY)
            ensureRouteWithTrips("Colombo", "Kandy", 3.25, busSP4004, 950.00, 360, null);   // 06:00 Non-AC 950
            ensureRouteWithTrips("Colombo", "Kandy", 3.25, bus1, 1250.00, 510, null);        // 08:30 AC 1250
            ensureRouteWithTrips("Colombo", "Kandy", 3.25, busND2002, 1650.00, 840, null);   // 14:00 Luxury 1650

            ensureRouteWithTrips("Kandy", "Colombo", 3.25, busSP4004, 950.00, 390, null);   // 06:30 Non-AC 950
            ensureRouteWithTrips("Kandy", "Colombo", 3.25, bus1, 1250.00, 540, null);        // 09:00 AC 1250
            ensureRouteWithTrips("Kandy", "Colombo", 3.25, busND2002, 1650.00, 870, null);   // 14:30 Luxury 1650

            // Colombo <-> Galle (2.0 hrs, varied types: NON_AC, AC, LUXURY)
            ensureRouteWithTrips("Colombo", "Galle", 2.00, busSP4004, 750.00, 390, null);   // 06:30 Non-AC 750
            ensureRouteWithTrips("Colombo", "Galle", 2.00, bus2, 950.00, 480, null);         // 08:00 Express AC 950
            ensureRouteWithTrips("Colombo", "Galle", 2.00, busND2002, 1350.00, 960, null);   // 16:00 Luxury 1350

            ensureRouteWithTrips("Galle", "Colombo", 2.00, bus2, 950.00, 450, null);         // 07:30 Express AC 950
            ensureRouteWithTrips("Galle", "Colombo", 2.00, busND2002, 1350.00, 990, null);   // 16:30 Luxury 1350

            // Colombo <-> Matara (2.5 hrs)
            ensureRouteWithTrips("Colombo", "Matara", 2.50, busNW8008, 800.00, 420, null);   // 07:00 Non-AC 800
            ensureRouteWithTrips("Colombo", "Matara", 2.50, busND1001, 1050.00, 960, null);  // 16:00 AC 1050
            ensureRouteWithTrips("Matara", "Colombo", 2.50, busNW8008, 800.00, 390, null);   // 06:30 Non-AC 800
            ensureRouteWithTrips("Matara", "Colombo", 2.50, busND1001, 1050.00, 900, null);  // 15:00 AC 1050

            // Colombo <-> Jaffna (8.0 hrs, varied types: AC, LUXURY, SLEEPER)
            ensureRouteWithTrips("Colombo", "Jaffna", 8.00, busND1001, 2400.00, 360, null);  // 06:00 AC 2400
            ensureRouteWithTrips("Colombo", "Jaffna", 8.00, busND2002, 2800.00, 480, null);  // 08:00 Luxury 2800
            ensureRouteWithTrips("Colombo", "Jaffna", 8.00, busWP3003, 3500.00, 1260, null); // 21:00 Sleeper 3500

            ensureRouteWithTrips("Jaffna", "Colombo", 8.00, busND2002, 2800.00, 540, null);  // 09:00 Luxury 2800
            ensureRouteWithTrips("Jaffna", "Colombo", 8.00, busWP3003, 3500.00, 1260, null); // 21:00 Sleeper 3500

            // Colombo <-> Nuwara Eliya (5.5 hrs, varied types: AC, LUXURY)
            ensureRouteWithTrips("Colombo", "Nuwara Eliya", 5.50, busCP5005, 1650.00, 390, null); // 06:30 AC 1650
            ensureRouteWithTrips("Colombo", "Nuwara Eliya", 5.50, busNE6006, 1950.00, 780, null); // 13:00 Luxury 1950
            ensureRouteWithTrips("Nuwara Eliya", "Colombo", 5.50, busCP5005, 1650.00, 420, null); // 07:00 AC 1650
            ensureRouteWithTrips("Nuwara Eliya", "Colombo", 5.50, busNE6006, 1950.00, 810, null); // 13:30 Luxury 1950

            // Colombo <-> Badulla (6.5 hrs, varied types: AC, SLEEPER)
            ensureRouteWithTrips("Colombo", "Badulla", 6.50, busCP5005, 2100.00, 420, null);  // 07:00 AC 2100
            ensureRouteWithTrips("Colombo", "Badulla", 6.50, busEP7007, 3200.00, 1290, null); // 21:30 Sleeper 3200
            ensureRouteWithTrips("Badulla", "Colombo", 6.50, busCP5005, 2100.00, 450, null);  // 07:30 AC 2100
            ensureRouteWithTrips("Badulla", "Colombo", 6.50, busEP7007, 3200.00, 1290, null); // 21:30 Sleeper 3200

            // Colombo <-> Anuradhapura (4.5 hrs, varied types: AC, LUXURY)
            ensureRouteWithTrips("Colombo", "Anuradhapura", 4.50, busND1001, 1500.00, 420, null); // 07:00 AC 1500
            ensureRouteWithTrips("Colombo", "Anuradhapura", 4.50, busND2002, 1850.00, 840, null); // 14:00 Luxury 1850
            ensureRouteWithTrips("Anuradhapura", "Colombo", 4.50, busND1001, 1500.00, 390, null); // 06:30 AC 1500
            ensureRouteWithTrips("Anuradhapura", "Colombo", 4.50, busND2002, 1850.00, 780, null); // 13:00 Luxury 1850

            // Colombo <-> Trincomalee (6.5 hrs, varied types: AC, SLEEPER)
            ensureRouteWithTrips("Colombo", "Trincomalee", 6.50, busND1001, 1850.00, 390, null);  // 06:30 AC 1850
            ensureRouteWithTrips("Colombo", "Trincomalee", 6.50, busEP7007, 3100.00, 1260, null); // 21:00 Sleeper 3100
            ensureRouteWithTrips("Trincomalee", "Colombo", 6.50, busND1001, 1850.00, 420, null);  // 07:00 AC 1850
            ensureRouteWithTrips("Trincomalee", "Colombo", 6.50, busEP7007, 3100.00, 1290, null); // 21:30 Sleeper 3100

            // Cross-Corridor & Regional Routes (LUXURY, NON_AC, AC)
            ensureRouteWithTrips("Kandy", "Galle", 4.50, busND2002, 2200.00, 420, null);       // 07:00 Luxury 2200
            ensureRouteWithTrips("Galle", "Kandy", 4.50, busND2002, 2200.00, 480, null);       // 08:00 Luxury 2200
            ensureRouteWithTrips("Kandy", "Jaffna", 7.00, busND2002, 2400.00, 420, null);      // 07:00 Luxury 2400
            ensureRouteWithTrips("Jaffna", "Kandy", 7.00, busND2002, 2400.00, 480, null);      // 08:00 Luxury 2400

            ensureRouteWithTrips("Kandy", "Nuwara Eliya", 2.50, busSP4004, 550.00, 480, null);  // 08:00 Non-AC 550
            ensureRouteWithTrips("Kandy", "Nuwara Eliya", 2.50, busCP5005, 850.00, 840, null);  // 14:00 AC 850
            ensureRouteWithTrips("Nuwara Eliya", "Kandy", 2.50, busSP4004, 550.00, 540, null);  // 09:00 Non-AC 550
            ensureRouteWithTrips("Nuwara Eliya", "Kandy", 2.50, busCP5005, 850.00, 900, null);  // 15:00 AC 850

            ensureRouteWithTrips("Galle", "Matara", 1.00, busSP4004, 450.00, 510, null);       // 08:30 Non-AC 450
            ensureRouteWithTrips("Matara", "Galle", 1.00, busSP4004, 450.00, 540, null);       // 09:00 Non-AC 450
        } catch (Exception ex) {
            log.warn("Non-fatal: Error seeding intercity routes and trips: {}", ex.getMessage());
        }
    }

    private void ensureRouteWithTrips(String src, String dest, double duration, Long busId, double fare, int depMin1, Integer depMin2) {
        Long routeId = oneLong("SELECT TOP 1 route_id FROM dbo.routes WHERE LOWER(source_city) = LOWER(?) AND LOWER(destination_city) = LOWER(?)", src, dest);
        if (routeId == null) {
            jdbc.update("INSERT INTO dbo.routes (source_city, destination_city, estimated_duration_hours) VALUES (?, ?, ?)", src, dest, duration);
            routeId = oneLong("SELECT TOP 1 route_id FROM dbo.routes WHERE LOWER(source_city) = LOWER(?) AND LOWER(destination_city) = LOWER(?)", src, dest);
        }

        if (routeId == null || busId == null) return;

        Integer upcomingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.trips WHERE route_id = ? AND bus_id = ? AND departure_time >= CAST(GETDATE() AS DATE)",
                Integer.class, routeId, busId);

        if (upcomingCount != null && upcomingCount >= 10) return;

        LocalDate today = LocalDate.now();
        int durMins = (int) Math.round(duration * 60);

        for (int offset = 0; offset <= 14; offset++) {
            LocalDate d = today.plusDays(offset);
            LocalDateTime dep1 = d.atStartOfDay().plusMinutes(depMin1);
            LocalDateTime arr1 = dep1.plusMinutes(durMins);

            Integer exists1 = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM dbo.trips WHERE route_id = ? AND departure_time = ?",
                    Integer.class, routeId, dep1);
            if (exists1 == null || exists1 == 0) {
                jdbc.update("INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, ?, 'SCHEDULED')",
                        busId, routeId, dep1, arr1, fare);
            }

            if (depMin2 != null) {
                LocalDateTime dep2 = d.atStartOfDay().plusMinutes(depMin2);
                LocalDateTime arr2 = dep2.plusMinutes(durMins);

                Integer exists2 = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM dbo.trips WHERE route_id = ? AND departure_time = ?",
                        Integer.class, routeId, dep2);
                if (exists2 == null || exists2 == 0) {
                    jdbc.update("INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status) VALUES (?, ?, ?, ?, ?, 'SCHEDULED')",
                            busId, routeId, dep2, arr2, fare);
                }
            }
        }
    }

    private long ensureUser(String name, String email, String phone, String password, String role) {
        Long id = oneLong("SELECT TOP 1 user_id FROM dbo.users WHERE LOWER(email) = LOWER(?)", email);
        String hashed = encoder.encode(password);
        if (id != null) {
            jdbc.update("UPDATE dbo.users SET full_name = ?, password_hash = ?, role = ? WHERE user_id = ?", name, hashed, role, id);
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

    private Long ensureBus(String busNumber, String busType, int totalSeats, String operatorName) {
        Long busId = oneLong("SELECT TOP 1 bus_id FROM dbo.buses WHERE bus_number = ?", busNumber);
        if (busId == null) {
            jdbc.update("INSERT INTO dbo.buses(bus_number, bus_type, total_seats, operator_name) VALUES (?, ?, ?, ?)",
                    busNumber, busType, totalSeats, operatorName);
            busId = oneLong("SELECT TOP 1 bus_id FROM dbo.buses WHERE bus_number = ?", busNumber);
        } else {
            jdbc.update("UPDATE dbo.buses SET bus_type = ?, operator_name = ?, total_seats = ? WHERE bus_id = ?",
                    busType, operatorName, totalSeats, busId);
        }
        ensureSeatsForBus(busId, totalSeats);
        return busId;
    }

    private void ensureSeatsForBus(Long busId, int totalSeats) {
        if (busId == null) return;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.seats WHERE bus_id = ?", Integer.class, busId);
        if (count == null || count == 0) {
            for (int i = 1; i <= totalSeats; i++) {
                boolean isWindow = (i % 4 == 1 || i % 4 == 0);
                jdbc.update("INSERT INTO dbo.seats (bus_id, seat_number, deck_type, is_window_seat) VALUES (?, ?, 'LOWER', ?)",
                        busId, String.valueOf(i), isWindow ? 1 : 0);
            }
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

    private static final java.util.Set<String> SEED_DEMO_EMAILS = java.util.Set.of(
            "admin@busreservation.lk",
            "admin@catchbus.lk",
            "conductor.kandy@busreservation.lk",
            "conductor@catchbus.lk",
            "ruwan.conductor@catchbus.lk",
            "driver.kandy@busreservation.lk",
            "driver@catchbus.lk",
            "suresh.driver@catchbus.lk",
            "priyantha.driver@catchbus.lk",
            "kavindu@busreservation.lk",
            "kamal@busreservation.lk",
            "tharushi@busreservation.lk",
            "dilshan@busreservation.lk",
            "anura@busreservation.lk"
    );

    private void synchronizeUserPasswords() {
        try {
            java.util.List<java.util.Map<String, Object>> allUsers = jdbc.queryForList("SELECT user_id, full_name, email FROM dbo.users");
            for (java.util.Map<String, Object> u : allUsers) {
                String email = (String) u.get("email");
                if (email == null) continue;
                String lowerEmail = email.trim().toLowerCase();
                // Never overwrite passwords for newly created or custom user accounts
                if (!SEED_DEMO_EMAILS.contains(lowerEmail)) {
                    continue;
                }
                String name = (String) u.get("full_name");
                Long uid = ((Number) u.get("user_id")).longValue();
                String firstName = "user";
                if (lowerEmail.contains("admin") || (name != null && name.toLowerCase().contains("admin"))) {
                    firstName = "admin";
                } else if (lowerEmail.equals("conductor@catchbus.lk") || (name != null && name.toLowerCase().contains("sunil"))) {
                    firstName = "sunil";
                } else if (lowerEmail.equals("driver@catchbus.lk") || (name != null && name.toLowerCase().contains("chaminda"))) {
                    firstName = "chaminda";
                } else if (name != null && !name.trim().isEmpty() && !name.toLowerCase().startsWith("demo") && !name.toLowerCase().startsWith("system")) {
                    firstName = name.trim().split("\\s+")[0].toLowerCase();
                } else {
                    firstName = lowerEmail.split("@")[0].split("\\.")[0].toLowerCase();
                }
                String pwd = firstName + "@123";
                jdbc.update("UPDATE dbo.users SET password_hash = ? WHERE user_id = ?", encoder.encode(pwd), uid);
            }
            log.info("DemoDataInitializer: Successfully formatted and synchronized passwords for demo seed accounts (firstName@123).");
        } catch (Exception e) {
            log.warn("DemoDataInitializer: synchronizeUserPasswords note: {}", e.getMessage());
        }
    }

    private void cleanAndSeedRealisticBookings(long kavinduId, long kamalId, long tharushiId, long dilshanId, Long currentTrip, Long secondTrip, Long bus1) {
        try {
            // Check if Kavindu has unrealistic bookings with > 3 bookings or missing seats
            Integer bCount = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bookings WHERE user_id = ?", Integer.class, kavinduId);
            Integer seatCountForKavindu = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.booking_seats bs JOIN dbo.bookings b ON bs.booking_id = b.booking_id WHERE b.user_id = ?", Integer.class, kavinduId);
            if (bCount != null && (bCount != 3 || seatCountForKavindu == null || seatCountForKavindu < 5)) {
                log.info("Pruning and configuring realistic demo bookings for Kavindu (found {} bookings, {} seats)", bCount, seatCountForKavindu);
                java.util.List<Long> oldBkgIds = jdbc.queryForList("SELECT booking_id FROM dbo.bookings WHERE user_id = ?", Long.class, kavinduId);
                for (Long bId : oldBkgIds) {
                    try { jdbc.update("DELETE FROM dbo.user_notifications WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.notifications WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.refunds WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.payments WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.tickets WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.luggage WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.booking_seats WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                    try { jdbc.update("DELETE FROM dbo.bookings WHERE booking_id = ?", bId); } catch (Exception ignored) {}
                }
            }

            Integer kavinduRemaining = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bookings WHERE user_id = ?", Integer.class, kavinduId);
            if (kavinduRemaining == null || kavinduRemaining == 0) {
                // Booking 1: Upcoming Outbound Journey (Colombo -> Kandy, 2 seats)
                if (currentTrip != null) {
                    jdbc.update("INSERT INTO dbo.bookings (user_id, trip_id, total_amount, booking_status, booking_time, version) VALUES (?, ?, 2500.00, 'CONFIRMED', SYSUTCDATETIME(), 0)",
                            kavinduId, currentTrip);
                    Long bkg1 = oneLong("SELECT TOP 1 booking_id FROM dbo.bookings WHERE user_id = ? AND trip_id = ? ORDER BY booking_id DESC", kavinduId, currentTrip);
                    java.util.List<Long> seats = jdbc.queryForList("SELECT TOP 2 seat_id FROM dbo.seats WHERE bus_id = ? ORDER BY TRY_CAST(seat_number AS INT) ASC", Long.class, bus1);
                    if (seats.size() >= 2 && bkg1 != null) {
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 1250.00)", bkg1, currentTrip, seats.get(0));
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 1250.00)", bkg1, currentTrip, seats.get(1));
                    }
                    if (bkg1 != null) {
                        String ref1 = "CB" + String.format("%08d", bkg1);
                        jdbc.update("DELETE FROM dbo.luggage WHERE tag_number = 'LUG-20261004-771209'");
                        jdbc.update("INSERT INTO dbo.luggage (tag_number, bag_quantity, description, status, booking_id, user_id, created_at) VALUES ('LUG-20261004-771209', 1, 'Navy blue hard-case Samsonite spinner', 'LOADED', ?, ?, SYSUTCDATETIME())",
                                bkg1, kavinduId);
                        jdbc.update("""
                            INSERT INTO dbo.tickets (booking_id, ticket_token, qr_payload, passenger_name, passenger_email, passenger_phone, boarding_point, destination_point, verification_status, issued_at, approval_status)
                            VALUES (?, ?, ?, 'Kavindu Perera', 'kavindu@busreservation.lk', '+94719876543', 'Colombo', 'Kandy', 'ISSUED', SYSUTCDATETIME(), 'APPROVED')
                            """, bkg1, "TKT-" + ref1, "CATCHBUS:" + ref1);
                    }
                }

                // Booking 2: Return Upcoming Journey (Kandy -> Colombo, 2 seats)
                if (secondTrip != null) {
                    jdbc.update("INSERT INTO dbo.bookings (user_id, trip_id, total_amount, booking_status, booking_time, version) VALUES (?, ?, 2500.00, 'CONFIRMED', SYSUTCDATETIME(), 0)",
                            kavinduId, secondTrip);
                    Long bkg2 = oneLong("SELECT TOP 1 booking_id FROM dbo.bookings WHERE user_id = ? AND trip_id = ? ORDER BY booking_id DESC", kavinduId, secondTrip);
                    java.util.List<Long> seats = jdbc.queryForList("SELECT TOP 2 seat_id FROM dbo.seats WHERE bus_id = ? ORDER BY TRY_CAST(seat_number AS INT) DESC", Long.class, bus1);
                    if (seats.size() >= 2 && bkg2 != null) {
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 1250.00)", bkg2, secondTrip, seats.get(0));
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 1250.00)", bkg2, secondTrip, seats.get(1));
                    }
                    if (bkg2 != null) {
                        String ref2 = "CB" + String.format("%08d", bkg2);
                        jdbc.update("""
                            INSERT INTO dbo.tickets (booking_id, ticket_token, qr_payload, passenger_name, passenger_email, passenger_phone, boarding_point, destination_point, verification_status, issued_at, approval_status)
                            VALUES (?, ?, ?, 'Kavindu Perera', 'kavindu@busreservation.lk', '+94719876543', 'Kandy', 'Colombo', 'ISSUED', SYSUTCDATETIME(), 'APPROVED')
                            """, bkg2, "TKT-" + ref2, "CATCHBUS:" + ref2);
                    }
                }

                // Booking 3: Past Completed Journey (Southern Expressway, Colombo -> Galle, 1 seat)
                Long pastTrip = oneLong("SELECT TOP 1 trip_id FROM dbo.trips WHERE trip_status = 'COMPLETED' OR departure_time < SYSUTCDATETIME() ORDER BY departure_time DESC");
                if (pastTrip != null) {
                    jdbc.update("INSERT INTO dbo.bookings (user_id, trip_id, total_amount, booking_status, booking_time, version) VALUES (?, ?, 950.00, 'COMPLETED', DATEADD(day, -3, SYSUTCDATETIME()), 0)",
                            kavinduId, pastTrip);
                    Long bkg3 = oneLong("SELECT TOP 1 booking_id FROM dbo.bookings WHERE user_id = ? AND trip_id = ? ORDER BY booking_id DESC", kavinduId, pastTrip);
                    Long pastBus = oneLong("SELECT bus_id FROM dbo.trips WHERE trip_id = ?", pastTrip);
                    Long pastSeat = oneLong("SELECT TOP 1 seat_id FROM dbo.seats WHERE bus_id = ? ORDER BY TRY_CAST(seat_number AS INT) ASC", pastBus != null ? pastBus : bus1);
                    if (pastSeat == null) {
                        pastSeat = oneLong("SELECT TOP 1 seat_id FROM dbo.seats ORDER BY seat_id ASC");
                    }
                    if (pastSeat != null && bkg3 != null) {
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 950.00)", bkg3, pastTrip, pastSeat);
                        String ref3 = "CB" + String.format("%08d", bkg3);
                        jdbc.update("""
                            INSERT INTO dbo.tickets (booking_id, ticket_token, qr_payload, passenger_name, passenger_email, passenger_phone, boarding_point, destination_point, verification_status, issued_at, approval_status, verified_at, verified_by)
                            VALUES (?, ?, ?, 'Kavindu Perera', 'kavindu@busreservation.lk', '+94719876543', 'Colombo', 'Galle', 'VERIFIED', DATEADD(day, -3, SYSUTCDATETIME()), 'APPROVED', DATEADD(day, -3, SYSUTCDATETIME()), 'CON-001')
                            """, bkg3, "TKT-" + ref3, "CATCHBUS:" + ref3);
                    }
                }
                log.info("DemoDataInitializer: Successfully configured realistic commuter bookings for Kavindu (1 outbound + 1 return + 1 past).");
            }

            // Seed realistic small bookings for other passengers
            seedOtherPassengerBookings(kamalId, tharushiId, dilshanId);

        } catch (Exception ex) {
            log.warn("DemoDataInitializer cleanAndSeedRealisticBookings note: {}", ex.getMessage());
        }
    }

    private void seedOtherPassengerBookings(long kamalId, long tharushiId, long dilshanId) {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.bookings WHERE user_id IN (?, ?, ?)", Integer.class, kamalId, tharushiId, dilshanId);
            if (count != null && count > 0) return;

            java.util.List<Long> trips = jdbc.queryForList("SELECT TOP 6 trip_id FROM dbo.trips WHERE departure_time >= SYSUTCDATETIME() ORDER BY trip_id ASC", Long.class);
            long[] passengers = new long[]{kamalId, tharushiId, dilshanId};
            int pIdx = 0;
            for (Long tId : trips) {
                long pId = passengers[pIdx % passengers.length];
                pIdx++;
                Long busId = oneLong("SELECT bus_id FROM dbo.trips WHERE trip_id = ?", tId);
                if (busId == null) continue;
                java.util.List<Long> availSeats = jdbc.queryForList("""
                    SELECT TOP 2 s.seat_id FROM dbo.seats s
                    WHERE s.bus_id = ? AND s.seat_id NOT IN (SELECT seat_id FROM dbo.booking_seats WHERE trip_id = ?)
                    ORDER BY CAST(s.seat_number AS INT) ASC
                    """, Long.class, busId, tId);
                if (!availSeats.isEmpty()) {
                    jdbc.update("INSERT INTO dbo.bookings (user_id, trip_id, total_amount, booking_status, booking_time, version) VALUES (?, ?, ?, 'CONFIRMED', SYSUTCDATETIME(), 0)",
                            pId, tId, 1250.0 * availSeats.size());
                    Long bkg = oneLong("SELECT TOP 1 booking_id FROM dbo.bookings WHERE user_id = ? AND trip_id = ? ORDER BY booking_id DESC", pId, tId);
                    for (Long sId : availSeats) {
                        jdbc.update("INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price) VALUES (?, ?, ?, 1250.00)", bkg, tId, sId);
                    }
                }
            }
            log.info("DemoDataInitializer: Seeded realistic commuter bookings for other passengers.");
        } catch (Exception e) {
            log.warn("DemoDataInitializer seedOtherPassengerBookings note: {}", e.getMessage());
        }
    }
}
