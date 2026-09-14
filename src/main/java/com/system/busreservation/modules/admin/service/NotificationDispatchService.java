package com.system.busreservation.modules.admin.service;

/**
 * Sends the operational summary report to stakeholders via the Notification
 * Gateway (owned by Module 5 - notifications/feedback/luggage).
 * Coordinate the exact contract (REST call vs. shared internal service bean)
 * with that teammate before wiring this up for real; for now this is a thin
 * seam so Module 6 can compile and be demoed independently.
 */
public interface NotificationDispatchService {
    void dispatchOperationalSummary(String recipientRole, String subject, String bodySummary);
}
