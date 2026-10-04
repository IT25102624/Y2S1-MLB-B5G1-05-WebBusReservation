package com.system.busreservation.modules.booking.controller;

import com.system.busreservation.common.dto.ApiResponse;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.booking.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingController Unit Tests (Teammate 3 - Supun N. E. G. A.)")
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingController bookingController;

    @Test
    @DisplayName("Create Booking - Success")
    void testCreateBooking() {
        Map<String, Object> request = Map.of(
                "passengerName", "Supun Perera",
                "passengerEmail", "supun@catchbus.lk",
                "seats", List.of("S1", "S2")
        );
        Map<String, Object> created = Map.of(
                "reference", "CB00001234",
                "status", "CONFIRMED"
        );
        when(bookingService.createBooking(request)).thenReturn(created);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.createBooking(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("CB00001234", response.getBody().getData().get("reference"));
    }

    @Test
    @DisplayName("Get Booking by Reference - Success")
    void testGetBooking() {
        Map<String, Object> booking = Map.of("reference", "CB00001234", "status", "CONFIRMED");
        when(bookingService.getBooking("CB00001234")).thenReturn(booking);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.getBooking("CB00001234");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CB00001234", response.getBody().getData().get("reference"));
    }

    @Test
    @DisplayName("Update Booking - Success")
    void testUpdateBooking() {
        Map<String, Object> updateReq = Map.of(
                "passengerName", "Supun Updated",
                "passengerPhone", "0771234567"
        );
        Map<String, Object> updated = Map.of(
                "reference", "CB00001234",
                "passenger", updateReq,
                "status", "CONFIRMED"
        );
        when(bookingService.updateBooking("CB00001234", updateReq)).thenReturn(updated);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.updateBooking("CB00001234", updateReq);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("CB00001234", response.getBody().getData().get("reference"));
    }

    @Test
    @DisplayName("Cancel Booking - Success")
    void testCancelBooking() {
        Map<String, Object> cancelled = Map.of("reference", "CB00001234", "status", "CANCELLED");
        when(bookingService.cancelBooking("CB00001234")).thenReturn(cancelled);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.cancelBooking("CB00001234");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CANCELLED", response.getBody().getData().get("status"));
    }
}
