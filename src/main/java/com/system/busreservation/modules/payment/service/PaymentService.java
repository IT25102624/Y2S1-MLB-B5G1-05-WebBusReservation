package com.system.busreservation.modules.payment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Booking;
import com.system.busreservation.model.BookingSeat;
import com.system.busreservation.model.Seat;
import com.system.busreservation.model.Trip;
import com.system.busreservation.model.User;
import com.system.busreservation.model.enums.BookingStatus;
import com.system.busreservation.model.enums.UserRole;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.booking.event.BookingConfirmedEvent;
import com.system.busreservation.modules.booking.repository.BookingSeatRepository;
import com.system.busreservation.modules.payment.dto.PaymentRequest;
import com.system.busreservation.modules.payment.strategy.PaymentContext;
import com.system.busreservation.modules.payment.strategy.PaymentExecutionResult;
import com.system.busreservation.modules.payment.strategy.PaymentMethodStrategy;
import com.system.busreservation.modules.payment.strategy.PaymentStrategyFactory;
import com.system.busreservation.modules.search.repository.TripRepository;
import com.system.busreservation.modules.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final BigDecimal SERVICE_FEE = new BigDecimal("100.00");
    private static final BigDecimal REFUND_PROTECTION_FEE = new BigDecimal("150.00");
    private static final String QR_PREFIX = "CATCHBUS:TICKET:";

    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Map<String, Object> process(PaymentRequest request, String authenticatedEmail) {
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("Accept the payment and refund terms before continuing.");
        }
        Map<String, Object> selectedCard = null;
        if (request.savedCardId() != null) {
            if (authenticatedEmail == null) throw new IllegalArgumentException("Sign in to use a saved card.");
            selectedCard = savedCard(request.savedCardId(), authenticatedEmail);
        }

        // 1. Strategy Pattern: Resolve appropriate payment method strategy
        PaymentMethodStrategy paymentStrategy = paymentStrategyFactory.getStrategy(request.paymentMethod());

        Trip trip = tripRepository.findByIdWithDetails(request.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", request.tripId()));
        List<Long> distinctSeatIds = request.seatIds().stream().distinct().toList();
        if (distinctSeatIds.size() != request.seatIds().size()) {
            throw new IllegalArgumentException("The same seat cannot be selected more than once.");
        }
        List<Seat> seats = seatRepository.findAllById(distinctSeatIds);
        if (seats.size() != distinctSeatIds.size()
                || seats.stream().anyMatch(seat -> !seat.getBus().getBusId().equals(trip.getBus().getBusId()))) {
            throw new IllegalArgumentException("One or more selected seats do not belong to this trip.");
        }
        List<Long> bookedIds = bookingSeatRepository.findActiveBookedSeatIds(trip.getTripId());
        if (distinctSeatIds.stream().anyMatch(bookedIds::contains)) {
            throw new IllegalStateException("One or more selected seats were just booked. Refresh and choose another seat.");
        }

        User passenger = resolvePassenger(request, authenticatedEmail);
        BigDecimal seatSubtotal = trip.getBaseFare().multiply(BigDecimal.valueOf(seats.size()));
        boolean refundProtection = Boolean.TRUE.equals(request.refundProtection());
        BigDecimal refundFee = refundProtection ? REFUND_PROTECTION_FEE : BigDecimal.ZERO;
        BigDecimal total = seatSubtotal.add(SERVICE_FEE).add(refundFee);
        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .user(passenger).trip(trip).totalAmount(total).bookingStatus(BookingStatus.PENDING).build());
        try {
            bookingSeatRepository.saveAllAndFlush(seats.stream().map(seat -> BookingSeat.builder()
                    .booking(booking).trip(trip).seat(seat).seatPrice(trip.getBaseFare()).build()).toList());
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("A selected seat was booked by another passenger. Please select again.");
        }

        String transactionReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String ticketToken = UUID.randomUUID().toString();

        // 2. Strategy Pattern: Delegate payment processing to the resolved strategy
        PaymentContext paymentContext = PaymentContext.builder()
                .request(request)
                .totalAmount(total)
                .bookingId(booking.getBookingId())
                .authenticatedEmail(authenticatedEmail)
                .transactionReference(transactionReference)
                .selectedCard(selectedCard)
                .build();
        PaymentExecutionResult paymentResult = paymentStrategy.processPayment(paymentContext);

        jdbc.update("""
                INSERT INTO payments(booking_id,transaction_reference,card_brand,card_last_four,amount,payment_status,paid_at)
                VALUES(?,?,?,?,?,?,SYSUTCDATETIME())
                """, booking.getBookingId(), transactionReference,
                paymentResult.getCardBrand(),
                paymentResult.getCardLastFour(),
                total,
                paymentResult.getPaymentStatus());

        jdbc.update("""
                INSERT INTO tickets(booking_id,ticket_token,qr_payload,passenger_name,passenger_email,passenger_phone,
                                    boarding_point,destination_point,verification_status,issued_at)
                VALUES(?,?,?,?,?,?,?,?,'VALID',SYSUTCDATETIME())
                """, booking.getBookingId(), ticketToken, QR_PREFIX + ticketToken, request.passengerName().trim(),
                cleanEmail(request.email()), request.phoneNumber().trim(), request.boardingPoint().trim(),
                request.destinationPoint().trim());
        jdbc.update("UPDATE tickets SET approval_status='APPROVED', refund_fee_paid=? WHERE booking_id=?",
                refundProtection, booking.getBookingId());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(booking);

        // 3. Observer Pattern: Broadcast booking confirmed event to all subscribed listeners
        eventPublisher.publishEvent(BookingConfirmedEvent.builder()
                .bookingId(booking.getBookingId())
                .userId(passenger.getUserId())
                .passengerName(request.passengerName().trim())
                .passengerEmail(cleanEmail(request.email()))
                .passengerPhone(request.phoneNumber().trim())
                .ticketToken(ticketToken)
                .boardingPoint(request.boardingPoint().trim())
                .destinationPoint(request.destinationPoint().trim())
                .totalAmount(total)
                .paymentMethod(paymentStrategy.getDisplayName())
                .transactionReference(transactionReference)
                .confirmedAt(Instant.now())
                .build());

        String cardNumber = request.cardNumber() == null ? "" : request.cardNumber().replaceAll("\\s+", "");
        if (Boolean.TRUE.equals(request.saveCard()) && selectedCard == null && !cardNumber.isBlank()) {
            if (authenticatedEmail == null) throw new IllegalArgumentException("Sign in before saving a payment card.");
            saveCard(authenticatedEmail, request.cardholderName(), cardNumber, request.expiry(), true);
        }

        Map<String, Object> result = ticketDetails(ticketToken);
        result.put("seatSubtotal", seatSubtotal);
        result.put("serviceFee", SERVICE_FEE);
        result.put("refundProtectionFee", refundFee);
        result.put("qrDownloadUrl", "/api/v1/payments/tickets/" + ticketToken + "/qr.png");
        return result;
    }

    public byte[] generateQr(String token) {
        ticketDetails(token);
        try {
            var matrix = new QRCodeWriter().encode(QR_PREFIX + token, BarcodeFormat.QR_CODE, 420, 420);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate the QR ticket.");
        }
    }

    public Map<String, Object> scan(MultipartFile image, boolean admin) {
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Select a QR image to upload.");
        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            if (bufferedImage == null) throw new IllegalArgumentException("The uploaded file is not a readable image.");
            String payload = new MultiFormatReader().decode(new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)))).getText();
            String token = null;
            if (payload.startsWith(QR_PREFIX)) {
                token = payload.substring(QR_PREFIX.length()).trim();
            } else if (payload.contains("catchbus://ticket/")) {
                int idx = payload.indexOf("catchbus://ticket/");
                token = payload.substring(idx + "catchbus://ticket/".length()).trim();
                token = token.split("[^a-zA-Z0-9-]")[0];
            } else if (payload.trim().matches("^[0-9a-fA-F-]{32,36}$")) {
                token = payload.trim();
            } else {
                throw new IllegalArgumentException("This is not a CatchBus ticket QR code.");
            }
            Map<String, Object> details = ticketDetails(token);
            if (!admin && !"APPROVED".equals(details.get("approvalStatus")) && !"PENDING".equals(details.get("approvalStatus"))) {
                throw new IllegalStateException("This ticket is not authorized for passenger boarding.");
            }
            return details;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No valid ticket QR code was found in the image.");
        }
    }

    @Transactional
    public Map<String, Object> confirm(String token, String verifiedBy) {
        Map<String, Object> current = ticketDetails(token);
        if ("REFUNDED".equals(current.get("verificationStatus")) || "CANCELLED".equals(current.get("bookingStatus"))) {
            throw new IllegalStateException("Refund completed. This QR ticket is no longer valid for boarding.");
        }
        if (!"APPROVED".equals(current.get("approvalStatus")) && !"PENDING".equals(current.get("approvalStatus"))) {
            throw new IllegalStateException("An administrator must approve this ticket before conductor verification.");
        }
        if ("VERIFIED".equals(current.get("verificationStatus"))) return current;
        jdbc.update("""
                UPDATE tickets SET verification_status='VERIFIED', verified_at=SYSUTCDATETIME(), verified_by=?
                WHERE ticket_token=? AND verification_status='VALID'
                """, verifiedBy == null || verifiedBy.isBlank() ? "Staff" : verifiedBy.trim(), token);
        jdbc.update("""
                UPDATE bookings SET booking_status='BOARDED'
                WHERE booking_id=(SELECT booking_id FROM tickets WHERE ticket_token=?)
                """, token);
        return ticketDetails(token);
    }

    public Map<String, Object> ticketDetails(String token) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT t.ticket_token, t.passenger_name, t.passenger_email, t.passenger_phone,
                       t.boarding_point, t.destination_point, t.verification_status, t.issued_at,
                       t.verified_at, t.verified_by, t.approval_status, t.approved_at, t.approved_by,
                       t.refund_fee_paid, b.booking_id, b.total_amount, b.booking_status,
                       tr.trip_id, tr.departure_time, tr.arrival_time, bu.bus_number, bu.operator_name,
                       r.source_city, r.destination_city, p.transaction_reference, p.card_brand, p.card_last_four
                FROM tickets t JOIN bookings b ON b.booking_id=t.booking_id
                JOIN trips tr ON tr.trip_id=b.trip_id JOIN buses bu ON bu.bus_id=tr.bus_id
                JOIN routes r ON r.route_id=tr.route_id LEFT JOIN payments p ON p.booking_id=b.booking_id
                WHERE t.ticket_token=?
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ticketToken", rs.getString("ticket_token"));
            item.put("bookingId", rs.getLong("booking_id"));
            item.put("passengerName", rs.getString("passenger_name"));
            item.put("passengerEmail", rs.getString("passenger_email"));
            item.put("passengerPhone", rs.getString("passenger_phone"));
            item.put("boardingPoint", rs.getString("boarding_point"));
            item.put("destinationPoint", rs.getString("destination_point"));
            item.put("verificationStatus", rs.getString("verification_status"));
            item.put("issuedAt", rs.getTimestamp("issued_at").toLocalDateTime());
            item.put("verifiedAt", rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime());
            item.put("verifiedBy", rs.getString("verified_by"));
            item.put("approvalStatus", rs.getString("approval_status"));
            item.put("approvedAt", rs.getTimestamp("approved_at") == null ? null : rs.getTimestamp("approved_at").toLocalDateTime());
            item.put("approvedBy", rs.getString("approved_by"));
            item.put("refundFeePaid", rs.getBoolean("refund_fee_paid"));
            item.put("refundProtectionFee", rs.getBoolean("refund_fee_paid") ? REFUND_PROTECTION_FEE : BigDecimal.ZERO);
            item.put("totalAmount", rs.getBigDecimal("total_amount"));
            item.put("bookingStatus", rs.getString("booking_status"));
            item.put("tripId", rs.getLong("trip_id"));
            item.put("departureTime", rs.getTimestamp("departure_time").toLocalDateTime());
            item.put("arrivalTime", rs.getTimestamp("arrival_time").toLocalDateTime());
            item.put("busNumber", rs.getString("bus_number"));
            item.put("operatorName", rs.getString("operator_name"));
            item.put("sourceCity", rs.getString("source_city"));
            item.put("destinationCity", rs.getString("destination_city"));
            item.put("transactionReference", rs.getString("transaction_reference"));
            item.put("card", rs.getString("card_brand") == null ? null : rs.getString("card_brand") + " •••• " + rs.getString("card_last_four"));
            return item;
        }, token);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Ticket", "token", token);
        Map<String, Object> result = rows.getFirst();
        result.put("seats", jdbc.queryForList("""
                SELECT s.seat_number FROM booking_seats bs JOIN seats s ON s.seat_id=bs.seat_id
                WHERE bs.booking_id=? ORDER BY TRY_CAST(s.seat_number AS INT), s.seat_number
                """, String.class, result.get("bookingId")));
        List<Map<String, Object>> refund = jdbc.queryForList("""
                SELECT TOP 1 refund_id refundId, refund_amount refundAmount, refund_status refundStatus,
                       requested_at requestedAt, approved_at approvedAt, processed_at processedAt, admin_notes adminNotes
                FROM refunds WHERE booking_id=? ORDER BY refund_id DESC
                """, result.get("bookingId"));
        result.put("refund", refund.isEmpty() ? null : refund.getFirst());

        List<Map<String, Object>> luggage = jdbc.queryForList("""
                SELECT luggage_id luggageId, tag_number tagNumber, tag_number tagReference,
                       bag_quantity bagQuantity, bag_quantity bagCount, status, description
                FROM luggage WHERE booking_id=?
                """, result.get("bookingId"));
        result.put("luggage", luggage);
        return result;
    }

    @Transactional
    public Map<String, Object> approveTicket(String token, String adminEmail) {
        int updated = jdbc.update("""
                UPDATE tickets SET approval_status='APPROVED', approved_at=SYSUTCDATETIME(), approved_by=?
                WHERE ticket_token=? AND approval_status='PENDING'
                """, adminEmail, token);
        if (updated == 0 && jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE ticket_token=?", Integer.class, token) == 0) {
            throw new ResourceNotFoundException("Ticket", "token", token);
        }
        return ticketDetails(token);
    }

    @Transactional
    public Map<String, Object> createRefund(String token, String adminEmail, String reason) {
        Map<String, Object> ticket = ticketDetails(token);
        if (!Boolean.TRUE.equals(ticket.get("refundFeePaid"))) {
            throw new IllegalStateException("This passenger did not purchase refund protection.");
        }
        long bookingId = ((Number) ticket.get("bookingId")).longValue();
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE booking_id=?", Integer.class, bookingId);
        if (existing != null && existing > 0) throw new IllegalStateException("A refund already exists for this booking.");
        BigDecimal refundable = ((BigDecimal) ticket.get("totalAmount")).subtract(REFUND_PROTECTION_FEE);
        jdbc.update("""
                INSERT INTO refunds(booking_id,refund_amount,refund_status,reason,requested_by,requested_at)
                VALUES(?,?,'REQUESTED',?,?,SYSUTCDATETIME())
                """, bookingId, refundable, reason == null ? "Passenger refund request" : reason.trim(), adminEmail);
        return ticketDetails(token);
    }

    @Transactional
    public Map<String, Object> requestRefund(String email, Map<String, String> request) {
        User passenger = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        long bookingId;
        try { bookingId = Long.parseLong(request.getOrDefault("bookingId", "")); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a valid booking number."); }

        List<Map<String, Object>> eligible = jdbc.queryForList("""
                SELECT t.ticket_token FROM bookings b JOIN tickets t ON t.booking_id=b.booking_id
                WHERE b.booking_id=? AND b.user_id=? AND t.refund_fee_paid=1
                """, bookingId, passenger.getUserId());
        if (eligible.isEmpty()) throw new IllegalStateException("This booking is not eligible for a refund.");
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE booking_id=?", Integer.class, bookingId);
        if (existing != null && existing > 0) throw new IllegalStateException("A refund request already exists for this booking.");

        String holder = required(request, "accountHolderName", "Account holder name");
        String bank = required(request, "bankName", "Bank name");
        String branch = required(request, "branchName", "Branch name");
        String account = required(request, "accountNumber", "Account number");
        jdbc.update("""
                MERGE refund_bank_details AS target
                USING (SELECT ? AS user_id) AS source ON target.user_id=source.user_id
                WHEN MATCHED THEN UPDATE SET account_holder_name=?,bank_name=?,branch_name=?,account_number=?,updated_at=SYSUTCDATETIME()
                WHEN NOT MATCHED THEN INSERT(user_id,account_holder_name,bank_name,branch_name,account_number,created_at,updated_at)
                VALUES(?,?,?,?,?,SYSUTCDATETIME(),SYSUTCDATETIME());
                """, passenger.getUserId(), holder, bank, branch, account,
                passenger.getUserId(), holder, bank, branch, account);
        Long bankId = jdbc.queryForObject("SELECT bank_detail_id FROM refund_bank_details WHERE user_id=?", Long.class, passenger.getUserId());
        BigDecimal total = jdbc.queryForObject("SELECT total_amount FROM bookings WHERE booking_id=?", BigDecimal.class, bookingId);
        BigDecimal refundable = total.subtract(REFUND_PROTECTION_FEE);
        jdbc.update("""
                INSERT INTO refunds(booking_id,bank_detail_id,refund_amount,refund_status,reason,requested_by,requested_at)
                VALUES(?,? ,?,'REQUESTED',?,?,SYSUTCDATETIME())
                """, bookingId, bankId, refundable, request.getOrDefault("reason", "Passenger cancellation"), email);
        jdbc.update("""
                INSERT INTO user_notifications(recipient_user_id,booking_id,notification_type,subject,message,is_read,created_at)
                SELECT user_id,?,'REFUND_REQUEST','New refund request',?,0,SYSUTCDATETIME()
                FROM users WHERE role='ADMIN'
                """, bookingId, "Passenger: " + passenger.getFullName() + "\nBooking: #" + bookingId
                + "\nRefund amount: LKR " + refundable + "\nAccount holder: " + holder
                + "\nBank: " + bank + "\nBranch: " + branch + "\nAccount number: " + account
                + "\nReason: " + request.getOrDefault("reason", "Passenger cancellation"));
        notifyUser(passenger.getUserId(), bookingId, "REFUND_REQUEST_SENT", "Refund request sent",
                "Your refund request for booking #" + bookingId + " was sent successfully.\nRefund amount: LKR "
                        + refundable + "\nBank: " + bank + " - " + branch + "\nAccount: " + account);
        return Map.of("bookingId", bookingId, "refundStatus", "REQUESTED", "refundAmount", refundable,
                "message", "Refund request sent successfully.");
    }

    public List<Map<String, Object>> inbox(String email) {
        return jdbc.queryForList("""
                SELECT n.notification_id notificationId, n.booking_id bookingId, n.notification_type notificationType,
                       n.subject, n.message, n.is_read isRead, n.created_at createdAt, t.ticket_token ticketToken,
                       r.refund_status refundStatus
                FROM user_notifications n LEFT JOIN tickets t ON t.booking_id=n.booking_id
                LEFT JOIN refunds r ON r.booking_id=n.booking_id
                WHERE n.recipient_user_id=(SELECT user_id FROM users WHERE email=?) ORDER BY n.created_at DESC
                """, email);
    }

    public Map<String, Object> bankDetails(String email) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT bank_detail_id bankDetailId, account_holder_name accountHolderName, bank_name bankName,
                       branch_name branchName, account_number accountNumber
                FROM refund_bank_details WHERE user_id=(SELECT user_id FROM users WHERE email=?)
                """, email);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    @Transactional
    public Map<String, Object> updateBankDetails(String email, Map<String, String> request) {
        User passenger = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        String holder = required(request, "accountHolderName", "Account holder name");
        String bank = required(request, "bankName", "Bank name");
        String branch = required(request, "branchName", "Branch name");
        String account = required(request, "accountNumber", "Account number");
        int updated = jdbc.update("""
                UPDATE refund_bank_details
                SET account_holder_name=?, bank_name=?, branch_name=?, account_number=?, updated_at=SYSUTCDATETIME()
                WHERE user_id=?
                """, holder, bank, branch, account, passenger.getUserId());
        if (updated == 0) throw new IllegalStateException("Submit a refund request before updating bank details.");

        List<Map<String, Object>> requests = jdbc.queryForList("""
                SELECT r.booking_id bookingId, r.refund_amount refundAmount, r.reason
                FROM refunds r JOIN bookings b ON b.booking_id=r.booking_id
                WHERE b.user_id=? AND r.refund_status='REQUESTED'
                """, passenger.getUserId());
        for (Map<String, Object> refund : requests) {
            long bookingId = ((Number) refund.get("bookingId")).longValue();
            String adminMessage = "Passenger: " + passenger.getFullName() + "\nBooking: #" + bookingId
                    + "\nRefund amount: LKR " + refund.get("refundAmount") + "\nAccount holder: " + holder
                    + "\nBank: " + bank + "\nBranch: " + branch + "\nAccount number: " + account
                    + "\nReason: " + refund.get("reason");
            jdbc.update("UPDATE user_notifications SET message=? WHERE booking_id=? AND notification_type='REFUND_REQUEST'",
                    adminMessage, bookingId);
            jdbc.update("""
                    UPDATE user_notifications SET message=?
                    WHERE recipient_user_id=? AND booking_id=? AND notification_type='REFUND_REQUEST_SENT'
                    """, "Your refund request for booking #" + bookingId + " was sent successfully.\nRefund amount: LKR "
                            + refund.get("refundAmount") + "\nBank: " + bank + " - " + branch + "\nAccount: " + account,
                    passenger.getUserId(), bookingId);
        }
        return bankDetails(email);
    }

    @Transactional
    public void markNotificationRead(long notificationId, String email) {
        jdbc.update("""
                UPDATE user_notifications SET is_read=1
                WHERE notification_id=? AND recipient_user_id=(SELECT user_id FROM users WHERE email=?)
                """, notificationId, email);
    }

    public List<Map<String, Object>> refunds() {
        return jdbc.queryForList("""
                SELECT r.refund_id refundId, r.booking_id bookingId, r.refund_amount refundAmount,
                       r.refund_status refundStatus, r.reason, r.requested_by requestedBy,
                       r.requested_at requestedAt, r.approved_by approvedBy, r.approved_at approvedAt,
                       r.processed_at processedAt, r.admin_notes adminNotes, t.ticket_token ticketToken,
                       t.passenger_name passengerName
                FROM refunds r JOIN tickets t ON t.booking_id=r.booking_id
                ORDER BY r.requested_at DESC
                """);
    }

    public List<Map<String, Object>> bookings() {
        return jdbc.queryForList("""
                SELECT b.booking_id bookingId, u.full_name passengerName, u.email passengerEmail,
                       u.phone_number passengerPhone, r.source_city sourceCity, r.destination_city destinationCity,
                       bu.bus_number busNumber, tr.departure_time departureTime,
                       COALESCE(seatData.seats, CASE WHEN b.booking_status='REFUNDED' THEN 'Released' ELSE '—' END) seats,
                       b.total_amount totalAmount, b.booking_status bookingStatus, b.booking_time bookingTime,
                       COALESCE(t.approval_status,'—') approvalStatus, COALESCE(t.refund_fee_paid,0) refundFeePaid,
                       COALESCE(p.payment_status,'—') paymentStatus,
                       COALESCE(p.transaction_reference,'—') transactionReference,
                       COALESCE(rf.refund_status,'—') refundStatus
                FROM bookings b JOIN users u ON u.user_id=b.user_id
                JOIN trips tr ON tr.trip_id=b.trip_id JOIN routes r ON r.route_id=tr.route_id
                JOIN buses bu ON bu.bus_id=tr.bus_id LEFT JOIN tickets t ON t.booking_id=b.booking_id
                LEFT JOIN payments p ON p.booking_id=b.booking_id LEFT JOIN refunds rf ON rf.booking_id=b.booking_id
                OUTER APPLY (SELECT STRING_AGG(CAST(s.seat_number AS NVARCHAR(MAX)), N', ') seats
                             FROM booking_seats bs JOIN seats s ON s.seat_id=bs.seat_id
                             WHERE bs.booking_id=b.booking_id) seatData
                ORDER BY b.booking_id DESC
                """);
    }

    @Transactional
    public Map<String, Object> updateRefund(long refundId, String action, String adminEmail, String notes) {
        String normalized = action == null ? "" : action.toUpperCase(Locale.ROOT);
        int updated;
        if ("APPROVE".equals(normalized)) {
            updated = jdbc.update("""
                    UPDATE refunds SET refund_status='REFUND', approved_by=?, approved_at=SYSUTCDATETIME(),
                    processed_at=SYSUTCDATETIME(), admin_notes='Approved from admin'
                    WHERE refund_id=? AND refund_status='REQUESTED'
                    """, adminEmail, refundId);
            if (updated > 0) completeRefund(refundId);
        } else if ("PROCESS".equals(normalized)) {
            updated = jdbc.update("""
                    UPDATE refunds SET refund_status='PROCESSED', processed_at=SYSUTCDATETIME(), admin_notes=COALESCE(?,admin_notes)
                    WHERE refund_id=? AND refund_status='APPROVED'
                    """, notes, refundId);
            if (updated > 0) jdbc.update("""
                    UPDATE bookings SET booking_status='CANCELLED'
                    WHERE booking_id=(SELECT booking_id FROM refunds WHERE refund_id=?)
                    """, refundId);
        } else if ("REJECT".equals(normalized)) {
            updated = jdbc.update("""
                    UPDATE refunds SET refund_status='REJECTED', approved_by=?, approved_at=SYSUTCDATETIME(), admin_notes=?
                    WHERE refund_id=? AND refund_status='REQUESTED'
                    """, adminEmail, notes, refundId);
        } else throw new IllegalArgumentException("Action must be APPROVE, PROCESS or REJECT.");
        if (updated == 0) throw new IllegalStateException("Refund action is not valid for its current status.");
        return jdbc.queryForMap("SELECT * FROM refunds WHERE refund_id=?", refundId);
    }

    @Transactional
    public void deleteRefund(long refundId) {
        if (jdbc.update("DELETE FROM refunds WHERE refund_id=? AND refund_status='REQUESTED'", refundId) == 0) {
            throw new IllegalStateException("Only a pending refund request can be deleted.");
        }
    }

    public List<Map<String, Object>> cards(String email) {
        return jdbc.queryForList("""
                SELECT card_id cardId, cardholder_name cardholderName, card_brand cardBrand,
                       card_last_four lastFour, expiry, is_default isDefault, created_at createdAt, updated_at updatedAt
                FROM payment_cards WHERE user_id=(SELECT user_id FROM users WHERE email=?) ORDER BY is_default DESC, card_id DESC
                """, email);
    }

    @Transactional
    public Map<String, Object> saveCard(String email, String holder, String number, String expiry, boolean makeDefault) {
        validateCard(number, expiry);
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email)).getUserId();
        if (makeDefault) jdbc.update("UPDATE payment_cards SET is_default=0 WHERE user_id=?", userId);
        jdbc.update("""
                INSERT INTO payment_cards(user_id,cardholder_name,card_brand,card_last_four,expiry,payment_token,is_default,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?,SYSUTCDATETIME(),SYSUTCDATETIME())
                """, userId, holder.trim(), cardBrand(number), number.substring(number.length()-4), expiry,
                "CARD-" + UUID.randomUUID(), makeDefault);
        return cards(email).getFirst();
    }

    @Transactional
    public Map<String, Object> updateCard(long cardId, String email, String holder, String expiry, boolean makeDefault) {
        validateExpiry(expiry);
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email)).getUserId();
        if (makeDefault) jdbc.update("UPDATE payment_cards SET is_default=0 WHERE user_id=?", userId);
        int updated = jdbc.update("""
                UPDATE payment_cards SET cardholder_name=?, expiry=?, is_default=?, updated_at=SYSUTCDATETIME()
                WHERE card_id=? AND user_id=?
                """, holder.trim(), expiry, makeDefault, cardId, userId);
        if (updated == 0) throw new ResourceNotFoundException("Payment card", "id", cardId);
        return cards(email).stream().filter(c -> ((Number)c.get("cardId")).longValue() == cardId).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteCard(long cardId, String email) {
        int deleted = jdbc.update("""
                DELETE FROM payment_cards WHERE card_id=? AND user_id=(SELECT user_id FROM users WHERE email=?)
                """, cardId, email);
        if (deleted == 0) throw new ResourceNotFoundException("Payment card", "id", cardId);
    }

    private Map<String, Object> savedCard(long cardId, String email) {
        List<Map<String, Object>> cards = jdbc.queryForList("""
                SELECT card_brand cardBrand, card_last_four lastFour, expiry
                FROM payment_cards WHERE card_id=? AND user_id=(SELECT user_id FROM users WHERE email=?)
                """, cardId, email);
        if (cards.isEmpty()) throw new ResourceNotFoundException("Payment card", "id", cardId);
        return cards.getFirst();
    }

    private void completeRefund(long refundId) {
        Map<String, Object> refund = jdbc.queryForMap("""
                SELECT r.booking_id, b.user_id, r.refund_amount FROM refunds r
                JOIN bookings b ON b.booking_id=r.booking_id WHERE r.refund_id=?
                """, refundId);
        long bookingId = ((Number) refund.get("booking_id")).longValue();
        long userId = ((Number) refund.get("user_id")).longValue();
        jdbc.update("DELETE FROM payments WHERE booking_id=?", bookingId);
        jdbc.update("UPDATE bookings SET booking_status='REFUNDED' WHERE booking_id=?", bookingId);
        jdbc.update("UPDATE tickets SET verification_status='REFUNDED' WHERE booking_id=?", bookingId);
        jdbc.update("DELETE FROM booking_seats WHERE booking_id=?", bookingId);
        notifyUser(userId, bookingId, "REFUND_SUCCESS", "Refund successful",
                "Your refund for booking #" + bookingId + " was processed successfully. The QR ticket is no longer valid.");
    }

    private void notifyUser(long userId, Long bookingId, String type, String subject, String message) {
        jdbc.update("""
                INSERT INTO user_notifications(recipient_user_id,booking_id,notification_type,subject,message,is_read,created_at)
                VALUES(?,?,?,?,?,0,SYSUTCDATETIME())
                """, userId, bookingId, type, subject, message);
    }

    private String required(Map<String, String> values, String key, String label) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    private User resolvePassenger(PaymentRequest request, String authenticatedEmail) {
        String normalized = cleanEmail(authenticatedEmail != null ? authenticatedEmail : request.email());
        if (normalized == null) normalized = "guest-" + UUID.randomUUID() + "@catchbus.local";
        final String email = normalized;
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .fullName(request.passengerName().trim()).email(email).phoneNumber(request.phoneNumber().trim())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())).role(UserRole.PASSENGER).build()));
    }

    private void validateCard(String number, String expiry) {
        if (!number.matches("\\d{16}")) {
            throw new IllegalArgumentException("The card number must contain exactly 16 digits.");
        }
        cardBrand(number);
        if (!passesLuhnCheck(number)) throw new IllegalArgumentException("Enter a valid Visa or Mastercard number.");
        validateExpiry(expiry);
    }

    private void validateExpiry(String expiry) {
        try {
            YearMonth exp = YearMonth.parse(expiry, DateTimeFormatter.ofPattern("MM/yy"));
            if (exp.isBefore(YearMonth.now())) throw new IllegalArgumentException("The card has expired.");
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Expiry must use MM/YY.");
        }
    }

    private String cardBrand(String card) {
        if (card.matches("4\\d{15}")) return "VISA";
        if (card.matches("5[1-5]\\d{14}")) return "MASTERCARD";
        if (card.matches("2\\d{15}")) {
            int prefix = Integer.parseInt(card.substring(0, 4));
            if (prefix >= 2221 && prefix <= 2720) return "MASTERCARD";
        }
        throw new IllegalArgumentException("Only standard Visa and Mastercard numbers are accepted.");
    }

    private boolean passesLuhnCheck(String card) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = card.length() - 1; i >= 0; i--) {
            int digit = card.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String cleanEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
