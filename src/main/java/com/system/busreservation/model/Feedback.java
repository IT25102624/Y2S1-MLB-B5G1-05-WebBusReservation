package com.system.busreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity representing customer feedback, ratings, and complaints.
 * Managed by Module 5 (Notifications, Feedback & Luggage Tracking).
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 30)
    private String type; // 'feedback' or 'complain'

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String description;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "complain_date")
    private LocalDate complainDate;

    @Column(name = "complain_time")
    private LocalTime complainTime;

    @Column(name = "reply", columnDefinition = "NVARCHAR(MAX)")
    private String reply;

    @PrePersist
    protected void onCreate() {
        if (this.complainDate == null) {
            this.complainDate = LocalDate.now();
        }
        if (this.complainTime == null) {
            this.complainTime = LocalTime.now();
        }
    }
}
