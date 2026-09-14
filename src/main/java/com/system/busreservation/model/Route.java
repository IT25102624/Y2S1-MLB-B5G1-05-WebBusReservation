package com.system.busreservation.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "source_city", nullable = false)
    private String sourceCity;

    @Column(name = "destination_city", nullable = false)
    private String destinationCity;

    @Column(name = "estimated_duration_hours")
    private BigDecimal estimatedDurationHours;
}