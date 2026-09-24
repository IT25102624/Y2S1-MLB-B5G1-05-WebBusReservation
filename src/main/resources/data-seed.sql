-- ==================================================================================
-- Bus Ticket Reservation System - Seed Data (Microsoft SQL Server / SSMS)
-- Populates Realistic Buses, Seats, Sri Lankan Routes, and Scheduled Trips
-- ==================================================================================

USE BusReservationDB;
GO

SET NOCOUNT ON;

-- Clear previous test data in reverse foreign key order
DELETE FROM dbo.booking_seats;
DELETE FROM dbo.bookings;
DELETE FROM dbo.trips;
DELETE FROM dbo.seats;
DELETE FROM dbo.routes;
DELETE FROM dbo.buses;
DELETE FROM dbo.users;
GO

-- 1. SEED USERS (BCrypt hashed passwords for 'Password@123')
INSERT INTO dbo.users (full_name, email, phone_number, password_hash, role, created_at)
VALUES 
    (N'System Administrator', N'admin@busreservation.lk', N'+94771234567', N'$2a$10$wNqvVvK.5sAec/3Kq4N6ueq6jWjUeO12eN1vN.r2g1Uf9PzO3lS8y', N'ADMIN', SYSUTCDATETIME()),
    (N'Dohan Imanjith', N'dohan@busreservation.lk', N'+94719876543', N'$2a$10$wNqvVvK.5sAec/3Kq4N6ueq6jWjUeO12eN1vN.r2g1Uf9PzO3lS8y', N'PASSENGER', SYSUTCDATETIME()),
    (N'Kasun Perera', N'conductor.kandy@busreservation.lk', N'+94762345678', N'$2a$10$wNqvVvK.5sAec/3Kq4N6ueq6jWjUeO12eN1vN.r2g1Uf9PzO3lS8y', N'CONDUCTOR', SYSUTCDATETIME()),
    (N'Nimal Silva', N'driver.kandy@busreservation.lk', N'+94763456789', N'$2a$10$wNqvVvK.5sAec/3Kq4N6ueq6jWjUeO12eN1vN.r2g1Uf9PzO3lS8y', N'DRIVER', SYSUTCDATETIME());
GO

-- 2. SEED BUSES
INSERT INTO dbo.buses (bus_number, bus_type, total_seats, operator_name)
VALUES 
    (N'ND-1001', N'AC', 40, N'Superline Express'),
    (N'ND-2002', N'LUXURY', 44, N'Rajarata Travels'),
    (N'WP-3003', N'SLEEPER', 36, N'Northern Highway Liner'),
    (N'SP-4004', N'NON_AC', 50, N'Southern Express'),
    (N'CP-5005', N'AC', 42, N'Hill Country Express');
GO

-- 3. SEED SEATS (Generate normalized seats for each bus)
DECLARE @BusId BIGINT;
DECLARE @TotalSeats INT;
DECLARE @SeatNum INT;
DECLARE @Deck NVARCHAR(20) = N'LOWER';
DECLARE @IsWindow BIT;

DECLARE bus_cursor CURSOR FOR 
SELECT bus_id, total_seats FROM dbo.buses;

OPEN bus_cursor;
FETCH NEXT FROM bus_cursor INTO @BusId, @TotalSeats;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @SeatNum = 1;
    WHILE @SeatNum <= @TotalSeats
    BEGIN
        IF (@SeatNum % 4 = 1 OR @SeatNum % 4 = 0)
            SET @IsWindow = 1;
        ELSE
            SET @IsWindow = 0;

        INSERT INTO dbo.seats (bus_id, seat_number, deck_type, is_window_seat)
        VALUES (@BusId, CAST(@SeatNum AS NVARCHAR(10)), @Deck, @IsWindow);

        SET @SeatNum = @SeatNum + 1;
    END;
    FETCH NEXT FROM bus_cursor INTO @BusId, @TotalSeats;
END;

CLOSE bus_cursor;
DEALLOCATE bus_cursor;
GO

-- 4. SEED ROUTES (Sri Lankan intercity express routes)
INSERT INTO dbo.routes (source_city, destination_city, estimated_duration_hours)
VALUES 
    (N'Colombo', N'Kandy', 3.50),
    (N'Kandy', N'Colombo', 3.50),
    (N'Colombo', N'Galle', 2.00),
    (N'Galle', N'Colombo', 2.00),
    (N'Colombo', N'Jaffna', 8.00),
    (N'Jaffna', N'Colombo', 8.00),
    (N'Colombo', N'Badulla', 6.50),
    (N'Badulla', N'Colombo', 6.50),
    (N'Colombo', N'Nuwara Eliya', 5.50),
    (N'Colombo', N'Anuradhapura', 4.50);
GO

-- 5. SEED TRIPS (Scheduled trips for today and upcoming 14 days)
DECLARE @Bus_ND1001 BIGINT = (SELECT bus_id FROM dbo.buses WHERE bus_number = N'ND-1001');
DECLARE @Bus_ND2002 BIGINT = (SELECT bus_id FROM dbo.buses WHERE bus_number = N'ND-2002');
DECLARE @Bus_WP3003 BIGINT = (SELECT bus_id FROM dbo.buses WHERE bus_number = N'WP-3003');
DECLARE @Bus_SP4004 BIGINT = (SELECT bus_id FROM dbo.buses WHERE bus_number = N'SP-4004');
DECLARE @Bus_CP5005 BIGINT = (SELECT bus_id FROM dbo.buses WHERE bus_number = N'CP-5005');

DECLARE @Route_Col_Kandy BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Kandy');
DECLARE @Route_Kandy_Col BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Kandy' AND destination_city = N'Colombo');
DECLARE @Route_Col_Galle BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Galle');
DECLARE @Route_Galle_Col BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Galle' AND destination_city = N'Colombo');
DECLARE @Route_Col_Jaffna BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Jaffna');
DECLARE @Route_Col_Badulla BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Badulla');
DECLARE @Route_Col_NuwaraEliya BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Nuwara Eliya');
DECLARE @Route_Col_Anuradhapura BIGINT = (SELECT route_id FROM dbo.routes WHERE source_city = N'Colombo' AND destination_city = N'Anuradhapura');

DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @DayOffset INT = 0;

WHILE @DayOffset <= 14
BEGIN
    DECLARE @TargetDate DATE = DATEADD(DAY, @DayOffset, @Today);

    -- Colombo -> Kandy (Morning AC, Afternoon Luxury, Evening AC)
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_ND1001, @Route_Col_Kandy, DATEADD(MINUTE, 390, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 600, CAST(@TargetDate AS DATETIME2)), 1200.00, N'SCHEDULED'),
        (@Bus_ND2002, @Route_Col_Kandy, DATEADD(MINUTE, 780, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 990, CAST(@TargetDate AS DATETIME2)), 1650.00, N'SCHEDULED'),
        (@Bus_CP5005, @Route_Col_Kandy, DATEADD(MINUTE, 1050, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 1260, CAST(@TargetDate AS DATETIME2)), 1200.00, N'SCHEDULED');

    -- Kandy -> Colombo
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_ND1001, @Route_Kandy_Col, DATEADD(MINUTE, 420, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 630, CAST(@TargetDate AS DATETIME2)), 1200.00, N'SCHEDULED'),
        (@Bus_CP5005, @Route_Kandy_Col, DATEADD(MINUTE, 900, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 1110, CAST(@TargetDate AS DATETIME2)), 1200.00, N'SCHEDULED');

    -- Colombo -> Galle (Expressway)
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_SP4004, @Route_Col_Galle, DATEADD(MINUTE, 450, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 570, CAST(@TargetDate AS DATETIME2)), 750.00, N'SCHEDULED'),
        (@Bus_ND1001, @Route_Col_Galle, DATEADD(MINUTE, 600, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 720, CAST(@TargetDate AS DATETIME2)), 950.00, N'SCHEDULED'),
        (@Bus_ND2002, @Route_Col_Galle, DATEADD(MINUTE, 960, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 1080, CAST(@TargetDate AS DATETIME2)), 1300.00, N'SCHEDULED');

    -- Colombo -> Jaffna (Overnight Sleeper & Day Express)
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_WP3003, @Route_Col_Jaffna, DATEADD(MINUTE, 1260, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 1740, CAST(@TargetDate AS DATETIME2)), 3200.00, N'SCHEDULED'),
        (@Bus_ND2002, @Route_Col_Jaffna, DATEADD(MINUTE, 480, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 960, CAST(@TargetDate AS DATETIME2)), 2600.00, N'SCHEDULED');

    -- Colombo -> Badulla
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_CP5005, @Route_Col_Badulla, DATEADD(MINUTE, 420, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 810, CAST(@TargetDate AS DATETIME2)), 2100.00, N'SCHEDULED');

    -- Colombo -> Nuwara Eliya
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_CP5005, @Route_Col_NuwaraEliya, DATEADD(MINUTE, 510, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 840, CAST(@TargetDate AS DATETIME2)), 1850.00, N'SCHEDULED');

    -- Colombo -> Anuradhapura
    INSERT INTO dbo.trips (bus_id, route_id, departure_time, arrival_time, base_fare, trip_status)
    VALUES 
        (@Bus_ND2002, @Route_Col_Anuradhapura, DATEADD(MINUTE, 540, CAST(@TargetDate AS DATETIME2)), DATEADD(MINUTE, 810, CAST(@TargetDate AS DATETIME2)), 1500.00, N'SCHEDULED');

    SET @DayOffset = @DayOffset + 1;
END;
GO

-- 6. SEED SAMPLE BOOKINGS (To verify available seat reduction and occupancy display)
DECLARE @PassengerId BIGINT = (SELECT TOP 1 user_id FROM dbo.users WHERE role = N'PASSENGER');
IF @PassengerId IS NOT NULL
BEGIN
    DECLARE @TId BIGINT, @BId BIGINT, @Fare DECIMAL(10,2), @TotalSeats INT;
    DECLARE @RowIdx INT = 0;
    DECLARE @SeatsToBook INT;

    DECLARE trip_cursor CURSOR FOR 
    SELECT t.trip_id, t.bus_id, t.base_fare, b.total_seats
    FROM dbo.trips t
    JOIN dbo.buses b ON t.bus_id = b.bus_id
    WHERE t.departure_time >= CAST(GETDATE() AS DATE)
      AND t.departure_time < DATEADD(DAY, 4, CAST(GETDATE() AS DATE))
    ORDER BY t.trip_id ASC;

    OPEN trip_cursor;
    FETCH NEXT FROM trip_cursor INTO @TId, @BId, @Fare, @TotalSeats;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @RowIdx = @RowIdx + 1;
        SET @SeatsToBook = CASE (@RowIdx % 7)
            WHEN 0 THEN 0
            WHEN 1 THEN 15
            WHEN 2 THEN 28
            WHEN 3 THEN 8
            WHEN 4 THEN 34
            WHEN 5 THEN 12
            WHEN 6 THEN 22
        END;

        IF @SeatsToBook > (@TotalSeats - 5)
            SET @SeatsToBook = @TotalSeats - 5;

        IF @SeatsToBook > 0
        BEGIN
            INSERT INTO dbo.bookings (user_id, trip_id, total_amount, booking_status, booking_time, version)
            VALUES (@PassengerId, @TId, @Fare * @SeatsToBook, N'CONFIRMED', SYSUTCDATETIME(), 0);

            DECLARE @BkgId BIGINT = SCOPE_IDENTITY();

            INSERT INTO dbo.booking_seats (booking_id, trip_id, seat_id, seat_price)
            SELECT TOP (@SeatsToBook) @BkgId, @TId, s.seat_id, @Fare
            FROM dbo.seats s
            WHERE s.bus_id = @BId
            ORDER BY CAST(s.seat_number AS INT) ASC;
        END;

        FETCH NEXT FROM trip_cursor INTO @TId, @BId, @Fare, @TotalSeats;
    END;

    CLOSE trip_cursor;
    DEALLOCATE trip_cursor;
END;
GO

PRINT 'Data seeding completed successfully! BusReservationDB is populated with buses, seats, routes, trips, and realistic seat bookings.';
GO
