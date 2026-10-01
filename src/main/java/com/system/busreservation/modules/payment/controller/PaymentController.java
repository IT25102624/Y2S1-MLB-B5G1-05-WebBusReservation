package com.system.busreservation.modules.payment.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.payment.dto.PaymentRequest;
import com.system.busreservation.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<Map<String, Object>>> process(@Valid @RequestBody PaymentRequest request,
                                                                     Authentication authentication) {
        String email = authenticatedEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success("Payment approved and ticket generated", paymentService.process(request, email)));
    }

    @GetMapping("/tickets/{token}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ticket(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.ticketDetails(token)));
    }

    @GetMapping(value = "/tickets/{token}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String token, @RequestParam(defaultValue = "false") boolean download) {
        HttpHeaders headers = new HttpHeaders();
        if (download) headers.setContentDisposition(ContentDisposition.attachment().filename("catchbus-ticket-" + token + ".png").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.IMAGE_PNG).body(paymentService.generateQr(token));
    }

    @PostMapping(value = "/tickets/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CONDUCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scan(@RequestPart("qrImage") MultipartFile qrImage,
                                                                  Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success("Ticket QR decoded", paymentService.scan(qrImage, admin)));
    }

    @PostMapping("/tickets/{token}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','CONDUCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirm(@PathVariable String token,
            @RequestBody(required = false) Map<String, String> request) {
        String verifiedBy = request == null ? "Staff" : request.getOrDefault("verifiedBy", "Staff");
        return ResponseEntity.ok(ApiResponse.success("Passenger boarding confirmed", paymentService.confirm(token, verifiedBy)));
    }

    @PostMapping("/tickets/{token}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable String token, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Ticket approved for conductor verification",
                paymentService.approveTicket(token, authentication.getName())));
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> refunds() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.refunds()));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> bookings() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.bookings()));
    }

    @PostMapping("/tickets/{token}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRefund(@PathVariable String token,
            @RequestBody(required = false) Map<String, String> request, Authentication authentication) {
        String reason = request == null ? null : request.get("reason");
        return ResponseEntity.ok(ApiResponse.success("Refund request created. Allow 7 business days for processing.",
                paymentService.createRefund(token, authentication.getName(), reason)));
    }

    @PatchMapping("/refunds/{refundId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRefund(@PathVariable long refundId,
            @RequestBody Map<String, String> request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund updated",
                paymentService.updateRefund(refundId, request.get("action"), authentication.getName(), request.get("notes"))));
    }

    @DeleteMapping("/refunds/{refundId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRefund(@PathVariable long refundId) {
        paymentService.deleteRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success("Pending refund deleted", null));
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cards(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.cards(authentication.getName())));
    }

    @PostMapping("/cards")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCard(@RequestBody Map<String, Object> request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Default payment card saved", paymentService.saveCard(
                authentication.getName(), String.valueOf(request.get("cardholderName")),
                String.valueOf(request.get("cardNumber")).replaceAll("\\s+", ""), String.valueOf(request.get("expiry")),
                !Boolean.FALSE.equals(request.get("makeDefault")))));
    }

    @PutMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCard(@PathVariable long cardId,
            @RequestBody Map<String, Object> request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Payment card updated in the database", paymentService.updateCard(
                cardId, authentication.getName(), String.valueOf(request.get("cardholderName")),
                String.valueOf(request.get("expiry")), !Boolean.FALSE.equals(request.get("makeDefault")))));
    }

    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable long cardId, Authentication authentication) {
        paymentService.deleteCard(cardId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Payment card deleted", null));
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> inbox(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.inbox(authentication.getName())));
    }

    @PatchMapping("/inbox/{notificationId}/read")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> readNotification(@PathVariable long notificationId,
                                                               Authentication authentication) {
        paymentService.markNotificationRead(notificationId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @GetMapping("/refund-bank-details")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bankDetails(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.bankDetails(authentication.getName())));
    }

    @PutMapping("/refund-bank-details")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateBankDetails(@RequestBody Map<String, String> request,
                                                                               Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund bank details updated in the existing database row",
                paymentService.updateBankDetails(authentication.getName(), request)));
    }

    @PostMapping("/refund-requests")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestRefund(@RequestBody Map<String, String> request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund request sent to the administrator",
                paymentService.requestRefund(authentication.getName(), request)));
    }

    private String authenticatedEmail(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName()) ? authentication.getName() : null;
    }
}
