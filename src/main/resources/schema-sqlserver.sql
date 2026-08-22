-- ==================================================================================
-- Bus Ticket Reservation System - Database Schema (Microsoft SQL Server / SSMS)
-- Normalized 3NF Schema Definition
-- ==================================================================================

-- Switch to master and ensure DB exists (Optional for initial setup)
-- CREATE DATABASE BusReservationDB;
-- GO
-- USE BusReservationDB;
-- GO

-- Drop tables in reverse order of foreign key dependencies if re-running
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL DROP TABLE dbo.booking_seats;
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL DROP TABLE dbo.bookings;
IF OBJECT_ID('dbo.trips', 'U') IS NOT NULL DROP TABLE dbo.trips;
IF OBJECT_ID('dbo.seats', 'U') IS NOT NULL DROP TABLE dbo.seats;
IF OBJECT_ID('dbo.routes', 'U') IS NOT NULL DROP TABLE dbo.routes;
IF OBJECT_ID('dbo.buses', 'U') IS NOT NULL DROP TABLE dbo.buses;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
GO

-- 1. USERS TABLE
CREATE TABLE dbo.users (
    user_id BIGINT IDENTITY(1,1) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(150) NOT NULL,
    phone_number NVARCHAR(20) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    role NVARCHAR(30) NOT NULL,
    created_at DATETIME2(7) NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_users PRIMARY KEY CLUSTERED (user_id),
    CONSTRAINT UQ_users_email UNIQUE (email)
);
GO

-- 2. BUSES TABLE
CREATE TABLE dbo.buses (
    bus_id BIGINT IDENTITY(1,1) NOT NULL,
    bus_number NVARCHAR(50) NOT NULL,
    bus_type NVARCHAR(30) NOT NULL,
    total_seats INT NOT NULL,
    operator_name NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_buses PRIMARY KEY CLUSTERED (bus_id),
    CONSTRAINT UQ_buses_bus_number UNIQUE (bus_number)
);
GO

-- 3. SEATS TABLE (Normalized Seat Layout per Bus)
CREATE TABLE dbo.seats (
    seat_id BIGINT IDENTITY(1,1) NOT NULL,
    bus_id BIGINT NOT NULL,
    seat_number NVARCHAR(10) NOT NULL,
    deck_type NVARCHAR(20) NOT NULL,
    is_window_seat BIT NOT NULL,
    CONSTRAINT PK_seats PRIMARY KEY CLUSTERED (seat_id),
    CONSTRAINT FK_seats_bus FOREIGN KEY (bus_id) REFERENCES dbo.buses(bus_id) ON DELETE CASCADE,
    CONSTRAINT uk_bus_seat_number UNIQUE (bus_id, seat_number)
);
GO

-- 4. ROUTES TABLE
CREATE TABLE dbo.routes (
    route_id BIGINT IDENTITY(1,1) NOT NULL,
    source_city NVARCHAR(100) NOT NULL,
    destination_city NVARCHAR(100) NOT NULL,
    estimated_duration_hours DECIMAL(4, 2) NOT NULL,
    CONSTRAINT PK_routes PRIMARY KEY CLUSTERED (route_id)
);
GO

-- 5. TRIPS TABLE (Bus + Route + Time Schedule)
CREATE TABLE dbo.trips (
    trip_id BIGINT IDENTITY(1,1) NOT NULL,
    bus_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    departure_time DATETIME2(7) NOT NULL,
    arrival_time DATETIME2(7) NOT NULL,
    base_fare DECIMAL(10, 2) NOT NULL,
    trip_status NVARCHAR(30) NOT NULL,
    CONSTRAINT PK_trips PRIMARY KEY CLUSTERED (trip_id),
    CONSTRAINT FK_trips_bus FOREIGN KEY (bus_id) REFERENCES dbo.buses(bus_id),
    CONSTRAINT FK_trips_route FOREIGN KEY (route_id) REFERENCES dbo.routes(route_id)
);
GO

-- 6. BOOKINGS TABLE (Header Transaction with Optimistic Locking Version)
CREATE TABLE dbo.bookings (
    booking_id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    booking_status NVARCHAR(30) NOT NULL,
    booking_time DATETIME2(7) NOT NULL CONSTRAINT DF_bookings_booking_time DEFAULT SYSUTCDATETIME(),
    version INT NOT NULL CONSTRAINT DF_bookings_version DEFAULT 0,
    CONSTRAINT PK_bookings PRIMARY KEY CLUSTERED (booking_id),
    CONSTRAINT FK_bookings_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id),
    CONSTRAINT FK_bookings_trip FOREIGN KEY (trip_id) REFERENCES dbo.trips(trip_id)
);
GO

-- 7. BOOKING_SEATS TABLE (Seat Reservation Items with Unique Trip+Seat Constraint to prevent double-booking)
CREATE TABLE dbo.booking_seats (
    booking_seat_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    seat_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT PK_booking_seats PRIMARY KEY CLUSTERED (booking_seat_id),
    CONSTRAINT FK_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT FK_booking_seats_trip FOREIGN KEY (trip_id) REFERENCES dbo.trips(trip_id),
    CONSTRAINT FK_booking_seats_seat FOREIGN KEY (seat_id) REFERENCES dbo.seats(seat_id),
    CONSTRAINT uk_trip_seat UNIQUE (trip_id, seat_id)
);
GO

-- Indexes for frequent query optimization
CREATE NONCLUSTERED INDEX IX_trips_schedule ON dbo.trips(route_id, departure_time, trip_status);
CREATE NONCLUSTERED INDEX IX_bookings_user ON dbo.bookings(user_id, booking_status);
CREATE NONCLUSTERED INDEX IX_booking_seats_trip ON dbo.booking_seats(trip_id, seat_id);
GO
