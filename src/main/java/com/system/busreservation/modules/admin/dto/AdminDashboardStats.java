package com.system.busreservation.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for aggregated Admin Dashboard statistics and overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStats {

    private long totalUsers;
    private long totalPassengers;
    private long totalConductors;
    private long totalDrivers;
    private long totalAdmins;
    private List<UserResponse> recentUsers;
}
