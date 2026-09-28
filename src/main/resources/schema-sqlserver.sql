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
IF OBJECT_ID('dbo.user_notifications', 'U') IS NOT NULL DROP TABLE dbo.user_notifications;
IF OBJECT_ID('dbo.refunds', 'U') IS NOT NULL DROP TABLE dbo.refunds;
IF OBJECT_ID('dbo.refund_bank_details', 'U') IS NOT NULL DROP TABLE dbo.refund_bank_details;
IF OBJECT_ID('dbo.payment_cards', 'U') IS NOT NULL DROP TABLE dbo.payment_cards;
IF OBJECT_ID('dbo.tickets', 'U') IS NOT NULL DROP TABLE dbo.tickets;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
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

-- 8. PAYMENTS TABLE (never stores full card number or CVV)
CREATE TABLE dbo.payments (
    payment_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    transaction_reference NVARCHAR(50) NOT NULL,
    card_brand NVARCHAR(30) NOT NULL,
    card_last_four CHAR(4) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_status NVARCHAR(30) NOT NULL,
    paid_at DATETIME2(7) NOT NULL CONSTRAINT DF_payments_paid_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_payments PRIMARY KEY CLUSTERED (payment_id),
    CONSTRAINT UQ_payments_booking UNIQUE (booking_id),
    CONSTRAINT UQ_payments_reference UNIQUE (transaction_reference),
    CONSTRAINT FK_payments_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- 9. DIGITAL QR TICKETS
CREATE TABLE dbo.tickets (
    ticket_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    ticket_token NVARCHAR(100) NOT NULL,
    qr_payload NVARCHAR(500) NOT NULL,
    passenger_name NVARCHAR(100) NOT NULL,
    passenger_email NVARCHAR(150) NULL,
    passenger_phone NVARCHAR(20) NOT NULL,
    boarding_point NVARCHAR(100) NOT NULL,
    destination_point NVARCHAR(100) NOT NULL,
    verification_status NVARCHAR(30) NOT NULL,
    issued_at DATETIME2(7) NOT NULL CONSTRAINT DF_tickets_issued_at DEFAULT SYSUTCDATETIME(),
    verified_at DATETIME2(7) NULL,
    verified_by NVARCHAR(100) NULL,
    approval_status NVARCHAR(30) NOT NULL CONSTRAINT DF_tickets_approval_status DEFAULT 'PENDING',
    approved_at DATETIME2(7) NULL,
    approved_by NVARCHAR(150) NULL,
    refund_fee_paid BIT NOT NULL CONSTRAINT DF_tickets_refund_fee_paid DEFAULT 0,
    CONSTRAINT PK_tickets PRIMARY KEY CLUSTERED (ticket_id),
    CONSTRAINT UQ_tickets_booking UNIQUE (booking_id),
    CONSTRAINT UQ_tickets_token UNIQUE (ticket_token),
    CONSTRAINT FK_tickets_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

CREATE TABLE dbo.payment_cards (
    card_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cardholder_name NVARCHAR(100) NOT NULL,
    card_brand NVARCHAR(30) NOT NULL,
    card_last_four CHAR(4) NOT NULL,
    expiry CHAR(5) NOT NULL,
    payment_token NVARCHAR(100) NOT NULL UNIQUE,
    is_default BIT NOT NULL CONSTRAINT DF_payment_cards_default DEFAULT 0,
    created_at DATETIME2(7) NOT NULL CONSTRAINT DF_payment_cards_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_payment_cards_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_payment_cards_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.refund_bank_details (
    bank_detail_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    account_holder_name NVARCHAR(120) NOT NULL,
    bank_name NVARCHAR(120) NOT NULL,
    branch_name NVARCHAR(120) NOT NULL,
    account_number NVARCHAR(50) NOT NULL,
    created_at DATETIME2(7) NOT NULL CONSTRAINT DF_refund_bank_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_refund_bank_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_refund_bank_user FOREIGN KEY(user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.refunds (
    refund_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    bank_detail_id BIGINT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_status NVARCHAR(30) NOT NULL,
    reason NVARCHAR(500) NULL,
    requested_by NVARCHAR(150) NOT NULL,
    requested_at DATETIME2(7) NOT NULL CONSTRAINT DF_refunds_requested DEFAULT SYSUTCDATETIME(),
    approved_by NVARCHAR(150) NULL,
    approved_at DATETIME2(7) NULL,
    processed_at DATETIME2(7) NULL,
    admin_notes NVARCHAR(1000) NULL,
    CONSTRAINT FK_refunds_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id),
    CONSTRAINT FK_refunds_bank_detail FOREIGN KEY (bank_detail_id) REFERENCES dbo.refund_bank_details(bank_detail_id),
    CONSTRAINT CK_refunds_status CHECK (refund_status IN ('REQUESTED','APPROVED','PROCESSED','REJECTED','REFUND'))
);
GO

CREATE TABLE dbo.user_notifications (
    notification_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    notification_type NVARCHAR(40) NOT NULL,
    subject NVARCHAR(150) NOT NULL,
    message NVARCHAR(1000) NOT NULL,
    is_read BIT NOT NULL CONSTRAINT DF_user_notifications_read DEFAULT 0,
    created_at DATETIME2(7) NOT NULL CONSTRAINT DF_user_notifications_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_user_notifications_user FOREIGN KEY(recipient_user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_user_notifications_booking FOREIGN KEY(booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- Indexes for frequent query optimization
CREATE NONCLUSTERED INDEX IX_trips_schedule ON dbo.trips(route_id, departure_time, trip_status);
CREATE NONCLUSTERED INDEX IX_bookings_user ON dbo.bookings(user_id, booking_status);
CREATE NONCLUSTERED INDEX IX_booking_seats_trip ON dbo.booking_seats(trip_id, seat_id);
CREATE NONCLUSTERED INDEX IX_tickets_token ON dbo.tickets(ticket_token, verification_status);
CREATE NONCLUSTERED INDEX IX_payment_cards_user ON dbo.payment_cards(user_id, is_default DESC);
CREATE NONCLUSTERED INDEX IX_refunds_status ON dbo.refunds(refund_status, requested_at DESC);
CREATE NONCLUSTERED INDEX IX_user_notifications_recipient ON dbo.user_notifications(recipient_user_id,is_read,created_at DESC);
GO
