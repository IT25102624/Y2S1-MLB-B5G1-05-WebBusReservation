package lk.catchbus.api.controller;

import lk.catchbus.api.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger")
public class PassengerController {
    private final AuthService auth;
    private final JdbcTemplate jdbc;

    public PassengerController(AuthService auth, JdbcTemplate jdbc) {
        this.auth = auth; this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(Authentication authentication) {
        return auth.passengerSummary((long) authentication.getPrincipal());
    }

    @GetMapping("/inbox")
    public List<Map<String,Object>> inbox(Authentication authentication) {
        return jdbc.queryForList("""
                SELECT n.notification_id notificationId,n.booking_id bookingId,n.notification_type notificationType,
                       n.subject,n.message,n.is_read isRead,n.created_at createdAt,t.ticket_token ticketToken,
                       r.refund_status refundStatus
                FROM user_notifications n LEFT JOIN tickets t ON t.booking_id=n.booking_id
                LEFT JOIN refunds r ON r.booking_id=n.booking_id
                WHERE n.recipient_user_id=? ORDER BY n.created_at DESC
                """, (long)authentication.getPrincipal());
    }

    @GetMapping("/refund-bank-details")
    public Map<String,Object> bankDetails(Authentication authentication) {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT account_holder_name accountHolderName,bank_name bankName,branch_name branchName,account_number accountNumber
                FROM refund_bank_details WHERE user_id=?
                """, (long)authentication.getPrincipal());
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    @PutMapping("/refund-bank-details")
    public Map<String,Object> updateBankDetails(@RequestBody Map<String,String> body, Authentication authentication) {
        long userId=(long)authentication.getPrincipal();
        String holder=required(body,"accountHolderName"),bank=required(body,"bankName"),branch=required(body,"branchName"),account=required(body,"accountNumber");
        int updated=jdbc.update("UPDATE refund_bank_details SET account_holder_name=?,bank_name=?,branch_name=?,account_number=?,updated_at=SYSUTCDATETIME() WHERE user_id=?",holder,bank,branch,account,userId);
        if(updated==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Submit a refund request before updating bank details.");
        String name=jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,userId);
        List<Map<String,Object>> requests=jdbc.queryForList("SELECT r.booking_id bookingId,r.refund_amount refundAmount,r.reason FROM refunds r JOIN bookings b ON b.booking_id=r.booking_id WHERE b.user_id=? AND r.refund_status='REQUESTED'",userId);
        for(Map<String,Object> refund:requests){
            long bookingId=((Number)refund.get("bookingId")).longValue();
            String adminMessage="Passenger: "+name+"\nBooking: #"+bookingId+"\nRefund amount: LKR "+refund.get("refundAmount")+"\nAccount holder: "+holder+"\nBank: "+bank+"\nBranch: "+branch+"\nAccount number: "+account+"\nReason: "+refund.get("reason");
            jdbc.update("UPDATE user_notifications SET message=? WHERE booking_id=? AND notification_type='REFUND_REQUEST'",adminMessage,bookingId);
            jdbc.update("UPDATE user_notifications SET message=? WHERE recipient_user_id=? AND booking_id=? AND notification_type='REFUND_REQUEST_SENT'","Your refund request for booking #"+bookingId+" was sent successfully.\nRefund amount: LKR "+refund.get("refundAmount")+"\nBank: "+bank+" - "+branch+"\nAccount: "+account,userId,bookingId);
        }
        return Map.of("success",true,"accountHolderName",holder,"bankName",bank,"branchName",branch,"accountNumber",account);
    }

    @PostMapping("/refunds")
    public Map<String,Object> requestRefund(@RequestBody Map<String,String> body, Authentication authentication) {
        long userId = (long)authentication.getPrincipal();
        long bookingId;
        try { bookingId = Long.parseLong(body.getOrDefault("bookingId", "")); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid booking number."); }
        Integer eligible = jdbc.queryForObject("SELECT COUNT(*) FROM bookings b JOIN tickets t ON t.booking_id=b.booking_id WHERE b.booking_id=? AND b.user_id=? AND t.refund_fee_paid=1", Integer.class, bookingId, userId);
        if (eligible == null || eligible == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is not eligible for a refund.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE booking_id=?", Integer.class, bookingId) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund request already exists for this booking.");
        String holder=required(body,"accountHolderName"),bank=required(body,"bankName"),branch=required(body,"branchName"),account=required(body,"accountNumber");
        jdbc.update("""
                MERGE refund_bank_details AS t USING(SELECT ? user_id) s ON t.user_id=s.user_id
                WHEN MATCHED THEN UPDATE SET account_holder_name=?,bank_name=?,branch_name=?,account_number=?,updated_at=SYSUTCDATETIME()
                WHEN NOT MATCHED THEN INSERT(user_id,account_holder_name,bank_name,branch_name,account_number,created_at,updated_at)
                VALUES(?,?,?,?,?,SYSUTCDATETIME(),SYSUTCDATETIME());
                """, userId,holder,bank,branch,account,userId,holder,bank,branch,account);
        Long bankId=jdbc.queryForObject("SELECT bank_detail_id FROM refund_bank_details WHERE user_id=?",Long.class,userId);
        BigDecimal total=jdbc.queryForObject("SELECT total_amount FROM bookings WHERE booking_id=?",BigDecimal.class,bookingId);
        BigDecimal amount=total.subtract(new BigDecimal("150.00"));
        String email=jdbc.queryForObject("SELECT email FROM users WHERE user_id=?",String.class,userId);
        jdbc.update("INSERT INTO refunds(booking_id,bank_detail_id,refund_amount,refund_status,reason,requested_by,requested_at) VALUES(?,?,?,'REQUESTED',?,?,SYSUTCDATETIME())",bookingId,bankId,amount,body.getOrDefault("reason","Passenger cancellation"),email);
        String name=jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,userId);
        String details="Passenger: "+name+"\nBooking: #"+bookingId+"\nRefund amount: LKR "+amount+"\nAccount holder: "+holder+"\nBank: "+bank+"\nBranch: "+branch+"\nAccount number: "+account+"\nReason: "+body.getOrDefault("reason","Passenger cancellation");
        jdbc.update("INSERT INTO user_notifications(recipient_user_id,booking_id,notification_type,subject,message,is_read,created_at) SELECT user_id,?,'REFUND_REQUEST','New refund request',?,0,SYSUTCDATETIME() FROM users WHERE role='ADMIN'",bookingId,details);
        jdbc.update("INSERT INTO user_notifications(recipient_user_id,booking_id,notification_type,subject,message,is_read,created_at) VALUES(?,?,'REFUND_REQUEST_SENT','Refund request sent',?,0,SYSUTCDATETIME())",userId,bookingId,"Your refund request for booking #"+bookingId+" was sent successfully.\nRefund amount: LKR "+amount+"\nBank: "+bank+" - "+branch+"\nAccount: "+account);
        return Map.of("success",true,"refundStatus","REQUESTED","refundAmount",amount);
    }

    private String required(Map<String,String> body,String key) {
        String value=body.get(key); if(value==null||value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete all bank details."); return value.trim();
    }
}
