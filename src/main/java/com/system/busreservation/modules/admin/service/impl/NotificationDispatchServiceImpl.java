package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final JavaMailSender mailSender;

    public NotificationDispatchServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void dispatchOperationalSummary(String toEmail, String subject, String bodySummary) {
        if (mailSender == null) {
            log.info("Mail sender not configured. Simulated dispatch of operational summary to [{}]: {}\n{}",
                    toEmail, subject, bodySummary);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(bodySummary);
            message.setFrom("operations@catchbus.lk");
            mailSender.send(message);
            log.info("Operational summary successfully emailed to [{}]", toEmail);
        } catch (Exception ex) {
            log.warn("Failed to dispatch email via SMTP to [{}]: {}. Logging summary locally instead.", toEmail, ex.getMessage());
        }
    }
}