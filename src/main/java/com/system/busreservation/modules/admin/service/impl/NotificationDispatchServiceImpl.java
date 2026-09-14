package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    @Override
    public void dispatchOperationalSummary(String recipientRole, String subject, String bodySummary) {
        // TODO: replace with a real call into modules.notification once Module 5
        // exposes its client/service bean or REST endpoint.
        log.info("[Notification Gateway] to={} subject='{}' summary='{}'", recipientRole, subject, bodySummary);
    }
}
