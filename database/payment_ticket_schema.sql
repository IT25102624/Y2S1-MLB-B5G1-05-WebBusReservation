USE BusReservationDB;
GO

IF OBJECT_ID('dbo.payments', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payments (
        payment_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        booking_id BIGINT NOT NULL UNIQUE,
        transaction_reference NVARCHAR(50) NOT NULL UNIQUE,
        card_brand NVARCHAR(30) NOT NULL,
        card_last_four CHAR(4) NOT NULL,
        amount DECIMAL(10,2) NOT NULL,
        payment_status NVARCHAR(30) NOT NULL,
        paid_at DATETIME2(7) NOT NULL CONSTRAINT DF_payments_paid_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_payments_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
    );
END;
GO

IF OBJECT_ID('dbo.tickets', 'U') IS NOT NULL AND COL_LENGTH('dbo.tickets', 'approval_status') IS NULL
BEGIN
    ALTER TABLE dbo.tickets ADD
        approval_status NVARCHAR(30) NOT NULL CONSTRAINT DF_tickets_approval_status DEFAULT 'PENDING',
        approved_at DATETIME2(7) NULL,
        approved_by NVARCHAR(150) NULL,
        refund_fee_paid BIT NOT NULL CONSTRAINT DF_tickets_refund_fee_paid DEFAULT 0;
END;
GO

IF OBJECT_ID('dbo.payment_cards', 'U') IS NULL
BEGIN
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
    CREATE INDEX IX_payment_cards_user ON dbo.payment_cards(user_id, is_default DESC);
END;
GO

IF OBJECT_ID('dbo.refunds', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.refunds (
        refund_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        booking_id BIGINT NOT NULL UNIQUE,
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
        CONSTRAINT CK_refunds_status CHECK (refund_status IN ('REQUESTED','APPROVED','PROCESSED','REJECTED','REFUND'))
    );
    CREATE INDEX IX_refunds_status ON dbo.refunds(refund_status, requested_at DESC);
END;
GO

IF OBJECT_ID('dbo.refund_bank_details', 'U') IS NULL
BEGIN
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
END;
GO

IF COL_LENGTH('dbo.refunds', 'bank_detail_id') IS NULL
    ALTER TABLE dbo.refunds ADD bank_detail_id BIGINT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_refunds_bank_detail')
    ALTER TABLE dbo.refunds ADD CONSTRAINT FK_refunds_bank_detail FOREIGN KEY(bank_detail_id) REFERENCES dbo.refund_bank_details(bank_detail_id);
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_refunds_status')
    ALTER TABLE dbo.refunds DROP CONSTRAINT CK_refunds_status;
ALTER TABLE dbo.refunds ADD CONSTRAINT CK_refunds_status CHECK
    (refund_status IN ('REQUESTED','APPROVED','PROCESSED','REJECTED','REFUND'));
UPDATE dbo.refunds SET refund_status='REFUND', admin_notes='Approved from admin' WHERE refund_status='PROCESSED';
GO

IF OBJECT_ID('dbo.user_notifications', 'U') IS NULL
BEGIN
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
    CREATE INDEX IX_user_notifications_recipient ON dbo.user_notifications(recipient_user_id,is_read,created_at DESC);
END;
GO

IF OBJECT_ID('dbo.tickets', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tickets (
        ticket_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        booking_id BIGINT NOT NULL UNIQUE,
        ticket_token NVARCHAR(100) NOT NULL UNIQUE,
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
        CONSTRAINT FK_tickets_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
    );
    CREATE INDEX IX_tickets_token ON dbo.tickets(ticket_token, verification_status);
END;
GO
