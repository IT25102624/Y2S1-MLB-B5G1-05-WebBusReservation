package com.system.busreservation.model;

import com.system.busreservation.model.enums.LuggageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entity representing luggage registered and tracked for a passenger's booking.
 * Managed by Module 5 (Notifications, Feedback & Luggage Tracking).
 */
@Entity
@Table(name = "luggage", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_luggage_tag_number", columnNames = {"tag_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"booking", "user"})
public class Luggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "luggage_id")
    private Long luggageId;

    @Column(name = "tag_number", nullable = false, length = 40)
    private String tagNumber;

    @Column(name = "bag_quantity", nullable = false)
    private Integer bagQuantity;

    @Column(name = "description", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LuggageStatus status = LuggageStatus.TAGGED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = LuggageStatus.TAGGED;
        }
    }
}
