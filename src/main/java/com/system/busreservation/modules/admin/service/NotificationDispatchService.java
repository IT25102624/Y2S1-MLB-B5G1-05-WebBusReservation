package com.system.busreservation.modules.admin.service;

public interface NotificationDispatchService {

    void dispatchOperationalSummary(String toEmail, String subject, String bodySummary);
}