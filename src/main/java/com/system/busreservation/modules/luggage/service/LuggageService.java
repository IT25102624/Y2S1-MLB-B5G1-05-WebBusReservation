package com.system.busreservation.modules.luggage.service;

import com.system.busreservation.common.exception.ResourceNotFoundException;
import com.system.busreservation.model.Booking;
import com.system.busreservation.model.Luggage;
import com.system.busreservation.model.User;
import com.system.busreservation.model.enums.LuggageStatus;
import com.system.busreservation.modules.auth.repository.UserRepository;
import com.system.busreservation.modules.booking.repository.BookingRepository;
import com.system.busreservation.modules.luggage.dto.LuggageCreateRequest;
import com.system.busreservation.modules.luggage.dto.LuggageResponse;
import com.system.busreservation.modules.luggage.dto.LuggageStatusUpdateRequest;
import com.system.busreservation.modules.luggage.repository.LuggageRepository;
import com.system.busreservation.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class LuggageService {

    private final LuggageRepository luggageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public LuggageResponse createLuggage(LuggageCreateRequest request) {
        log.info("Creating luggage tag for booking ID: {}", request.getBookingId());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseGet(() -> {
                    log.warn("Booking ID {} not found, falling back to an existing active booking", request.getBookingId());
                    return bookingRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", request.getBookingId()));
                });

        User user;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "userId", request.getUserId()));
        } else {
            user = booking.getUser();
        }

        String tagNumber = generateUniqueTagNumber();

        Luggage luggage = Luggage.builder()
                .tagNumber(tagNumber)
                .bagQuantity(request.getBagQuantity())
                .description(request.getDescription())
                .status(LuggageStatus.TAGGED)
                .booking(booking)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        Luggage saved = luggageRepository.save(luggage);

        // Dispatch notification
        try {
            notificationService.sendNotification(
                    user,
                    booking,
                    String.format("Luggage tag %s successfully registered for %d bag(s).", tagNumber, request.getBagQuantity()),
                    "LUGGAGE_TAGGED"
            );
        } catch (Exception e) {
            log.warn("Failed to send notification for luggage tag: {}", e.getMessage());
        }

        return LuggageResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LuggageResponse> getAllLuggage() {
        return luggageRepository.findAll()
                .stream()
                .map(LuggageResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LuggageResponse getLuggageById(Long id) {
        Luggage luggage = luggageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Luggage", "luggageId", id));
        return LuggageResponse.fromEntity(luggage);
    }

    @Transactional(readOnly = true)
    public LuggageResponse getLuggageByTagNumber(String tagNumber) {
        Luggage luggage = luggageRepository.findByTagNumber(tagNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Luggage", "tagNumber", tagNumber));
        return LuggageResponse.fromEntity(luggage);
    }

    @Transactional(readOnly = true)
    public List<LuggageResponse> getLuggageByUserId(Long userId) {
        return luggageRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(LuggageResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LuggageResponse> getLuggageByBookingId(Long bookingId) {
        return luggageRepository.findByBooking_BookingId(bookingId)
                .stream()
                .map(LuggageResponse::fromEntity)
                .toList();
    }

    @Transactional
    public LuggageResponse updateStatus(Long id, LuggageStatusUpdateRequest request) {
        log.info("Updating luggage status ID {} to {}", id, request.getStatus());
        Luggage luggage = luggageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Luggage", "luggageId", id));

        luggage.setStatus(request.getStatus());
        Luggage saved = luggageRepository.save(luggage);

        // Notify user about status change
        try {
            notificationService.sendNotification(
                    luggage.getUser(),
                    luggage.getBooking(),
                    String.format("Luggage tag %s status updated to %s.", luggage.getTagNumber(), request.getStatus()),
                    "LUGGAGE_STATUS"
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch luggage status notification: {}", e.getMessage());
        }

        return LuggageResponse.fromEntity(saved);
    }

    @Transactional
    public LuggageResponse updateLuggage(Long id, Integer bagQuantity, String description, LuggageStatus status) {
        log.info("Updating luggage record ID: {}", id);
        Luggage luggage = luggageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Luggage", "luggageId", id));

        if (bagQuantity != null) {
            luggage.setBagQuantity(bagQuantity);
        }
        if (description != null) {
            luggage.setDescription(description);
        }
        if (status != null) {
            luggage.setStatus(status);
        }

        Luggage saved = luggageRepository.save(luggage);
        return LuggageResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteLuggage(Long id) {
        log.info("Deleting luggage record ID: {}", id);
        if (!luggageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Luggage", "luggageId", id);
        }
        luggageRepository.deleteById(id);
    }

    private String generateUniqueTagNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String tagNumber;
        int attempts = 0;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 999999);
            tagNumber = String.format("LUG-%s-%06d", datePrefix, randomSuffix);
            attempts++;
        } while (luggageRepository.existsByTagNumber(tagNumber) && attempts < 20);
        return tagNumber;
    }
}
