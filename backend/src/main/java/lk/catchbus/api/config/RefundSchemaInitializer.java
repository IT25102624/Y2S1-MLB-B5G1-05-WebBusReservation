package lk.catchbus.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps an existing demo database compatible with the inbox/refund workflow. */
@Component
public class RefundSchemaInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    public RefundSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(String... args) {
        jdbc.execute("IF OBJECT_ID('dbo.tickets','U') IS NOT NULL AND COL_LENGTH('dbo.tickets','approval_status') IS NULL ALTER TABLE dbo.tickets ADD approval_status NVARCHAR(30) NOT NULL CONSTRAINT DF_tickets_approval_status DEFAULT 'PENDING'");
        jdbc.execute("IF OBJECT_ID('dbo.tickets','U') IS NOT NULL AND COL_LENGTH('dbo.tickets','approved_at') IS NULL ALTER TABLE dbo.tickets ADD approved_at DATETIME2(7) NULL");
        jdbc.execute("IF OBJECT_ID('dbo.tickets','U') IS NOT NULL AND COL_LENGTH('dbo.tickets','approved_by') IS NULL ALTER TABLE dbo.tickets ADD approved_by NVARCHAR(150) NULL");
        jdbc.execute("IF OBJECT_ID('dbo.tickets','U') IS NOT NULL AND COL_LENGTH('dbo.tickets','refund_fee_paid') IS NULL ALTER TABLE dbo.tickets ADD refund_fee_paid BIT NOT NULL CONSTRAINT DF_tickets_refund_fee_paid DEFAULT 0");
        jdbc.execute("""
            IF OBJECT_ID('dbo.refund_bank_details','U') IS NULL BEGIN
              CREATE TABLE dbo.refund_bank_details(
                bank_detail_id BIGINT IDENTITY(1,1) PRIMARY KEY,user_id BIGINT NOT NULL UNIQUE,
                account_holder_name NVARCHAR(120) NOT NULL,bank_name NVARCHAR(120) NOT NULL,
                branch_name NVARCHAR(120) NOT NULL,account_number NVARCHAR(50) NOT NULL,
                created_at DATETIME2(7) NOT NULL CONSTRAINT DF_refund_bank_created DEFAULT SYSUTCDATETIME(),
                updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_refund_bank_updated DEFAULT SYSUTCDATETIME(),
                CONSTRAINT FK_refund_bank_user FOREIGN KEY(user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE)
            END
            """);
        jdbc.execute("""
            IF OBJECT_ID('dbo.refunds','U') IS NULL BEGIN
              CREATE TABLE dbo.refunds(
                refund_id BIGINT IDENTITY(1,1) PRIMARY KEY,booking_id BIGINT NOT NULL UNIQUE,bank_detail_id BIGINT NULL,
                refund_amount DECIMAL(10,2) NOT NULL,refund_status NVARCHAR(30) NOT NULL,reason NVARCHAR(500) NULL,
                requested_by NVARCHAR(150) NOT NULL,requested_at DATETIME2(7) NOT NULL CONSTRAINT DF_refunds_requested DEFAULT SYSUTCDATETIME(),
                approved_by NVARCHAR(150) NULL,approved_at DATETIME2(7) NULL,processed_at DATETIME2(7) NULL,admin_notes NVARCHAR(1000) NULL,
                CONSTRAINT FK_refunds_booking FOREIGN KEY(booking_id) REFERENCES dbo.bookings(booking_id),
                CONSTRAINT FK_refunds_bank_detail FOREIGN KEY(bank_detail_id) REFERENCES dbo.refund_bank_details(bank_detail_id))
            END
            """);
        jdbc.execute("IF OBJECT_ID('dbo.refunds','U') IS NOT NULL AND COL_LENGTH('dbo.refunds','bank_detail_id') IS NULL ALTER TABLE dbo.refunds ADD bank_detail_id BIGINT NULL");
        jdbc.execute("IF OBJECT_ID('dbo.refunds','U') IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='FK_refunds_bank_detail') ALTER TABLE dbo.refunds ADD CONSTRAINT FK_refunds_bank_detail FOREIGN KEY(bank_detail_id) REFERENCES dbo.refund_bank_details(bank_detail_id)");
        jdbc.execute("""
            IF OBJECT_ID('dbo.refunds','U') IS NOT NULL BEGIN
              IF EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='CK_refunds_status')
                ALTER TABLE dbo.refunds DROP CONSTRAINT CK_refunds_status;
              ALTER TABLE dbo.refunds ADD CONSTRAINT CK_refunds_status CHECK(refund_status IN('REQUESTED','APPROVED','PROCESSED','REJECTED','REFUND'));
              UPDATE dbo.refunds SET refund_status='REFUND',admin_notes='Approved from admin' WHERE refund_status='PROCESSED';
            END
            """);
        jdbc.execute("""
            IF OBJECT_ID('dbo.refunds','U') IS NOT NULL AND OBJECT_ID('dbo.booking_seats','U') IS NOT NULL BEGIN
              UPDATE b SET booking_status='REFUNDED' FROM dbo.bookings b JOIN dbo.refunds r ON r.booking_id=b.booking_id WHERE r.refund_status='REFUND';
              DELETE p FROM dbo.payments p JOIN dbo.refunds r ON r.booking_id=p.booking_id WHERE r.refund_status='REFUND';
              DELETE bs FROM dbo.booking_seats bs JOIN dbo.refunds r ON r.booking_id=bs.booking_id WHERE r.refund_status='REFUND';
            END
            """);
        jdbc.execute("""
            IF OBJECT_ID('dbo.user_notifications','U') IS NULL BEGIN
              CREATE TABLE dbo.user_notifications(
                notification_id BIGINT IDENTITY(1,1) PRIMARY KEY,recipient_user_id BIGINT NOT NULL,booking_id BIGINT NULL,
                notification_type NVARCHAR(40) NOT NULL,subject NVARCHAR(150) NOT NULL,message NVARCHAR(1000) NOT NULL,
                is_read BIT NOT NULL CONSTRAINT DF_user_notifications_read DEFAULT 0,
                created_at DATETIME2(7) NOT NULL CONSTRAINT DF_user_notifications_created DEFAULT SYSUTCDATETIME(),
                CONSTRAINT FK_user_notifications_user FOREIGN KEY(recipient_user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE,
                CONSTRAINT FK_user_notifications_booking FOREIGN KEY(booking_id) REFERENCES dbo.bookings(booking_id))
            END
            """);
    }
}
