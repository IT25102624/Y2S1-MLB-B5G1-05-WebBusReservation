USE [BusReservationDB];
GO

IF OBJECT_ID('dbo.staff_profiles','U') IS NULL
BEGIN
    CREATE TABLE dbo.staff_profiles (
        user_id BIGINT NOT NULL PRIMARY KEY,
        employee_id NVARCHAR(50) NOT NULL UNIQUE,
        qr_token NVARCHAR(255) NOT NULL UNIQUE,
        active BIT NOT NULL CONSTRAINT DF_staff_profiles_active DEFAULT 1,
        CONSTRAINT FK_staff_profiles_user FOREIGN KEY(user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID('dbo.staff_assignments','U') IS NULL
BEGIN
    CREATE TABLE dbo.staff_assignments (
        assignment_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        trip_id BIGINT NOT NULL,
        role NVARCHAR(30) NOT NULL,
        assigned_from DATETIME2(7) NOT NULL CONSTRAINT DF_staff_assignments_from DEFAULT SYSUTCDATETIME(),
        assigned_to DATETIME2(7) NULL,
        active BIT NOT NULL CONSTRAINT DF_staff_assignments_active DEFAULT 1,
        CONSTRAINT FK_staff_assignments_user FOREIGN KEY(user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE,
        CONSTRAINT FK_staff_assignments_trip FOREIGN KEY(trip_id) REFERENCES dbo.trips(trip_id) ON DELETE CASCADE,
        CONSTRAINT CK_staff_assignments_role CHECK (role IN ('CONDUCTOR','DRIVER'))
    );
END
GO

IF OBJECT_ID('dbo.bus_location_updates','U') IS NULL
BEGIN
    CREATE TABLE dbo.bus_location_updates (
        location_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        trip_id BIGINT NOT NULL,
        bus_id BIGINT NOT NULL,
        latitude DECIMAL(10,7) NOT NULL,
        longitude DECIMAL(10,7) NOT NULL,
        speed_kmh DECIMAL(8,2) NULL,
        heading_degrees DECIMAL(6,2) NULL,
        recorded_at DATETIME2(7) NOT NULL CONSTRAINT DF_bus_location_recorded DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_bus_location_trip FOREIGN KEY(trip_id) REFERENCES dbo.trips(trip_id) ON DELETE CASCADE,
        CONSTRAINT FK_bus_location_bus FOREIGN KEY(bus_id) REFERENCES dbo.buses(bus_id)
    );
    CREATE INDEX IX_bus_location_trip_time ON dbo.bus_location_updates(trip_id, recorded_at DESC);
END
GO

IF OBJECT_ID('dbo.operations_messages','U') IS NULL
BEGIN
    CREATE TABLE dbo.operations_messages (
        message_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        sender_user_id BIGINT NOT NULL,
        subject NVARCHAR(120) NOT NULL,
        message NVARCHAR(2000) NOT NULL,
        urgent BIT NOT NULL CONSTRAINT DF_operations_messages_urgent DEFAULT 0,
        created_at DATETIME2(7) NOT NULL CONSTRAINT DF_operations_messages_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_operations_messages_sender FOREIGN KEY(sender_user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE
    );
    CREATE INDEX IX_operations_messages_created ON dbo.operations_messages(urgent DESC, created_at DESC);
END
GO

-- Optional: make existing role values explicit. Do not run if your instructor's
-- database uses different role names without adapting the check/queries.
-- Expected staff roles: CONDUCTOR, DRIVER, ADMIN. Passenger role: PASSENGER.
GO
