package lk.catchbus.api.controller;

import jakarta.validation.Valid;
import lk.catchbus.api.model.StaffDtos.BoardRequest;
import lk.catchbus.api.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conductor")
public class ConductorController {
    private final BookingService bookings;
    public ConductorController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping("/board")
    public Object board(@Valid @RequestBody BoardRequest request, Authentication authentication) {
        return bookings.board((long) authentication.getPrincipal(), request.bookingReference(), request.remarks());
    }
}
