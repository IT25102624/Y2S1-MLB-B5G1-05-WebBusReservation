package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.modules.admin.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final JavaMailSender mailSender;

    @Override
    public void dispatchOperationalSummary(String toEmail, String subject, String bodySummary) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(bodySummary);
        message.setFrom("your-email@gmail.com");   // ඔයාගේ Gmail එක

        mailSender.send(message);
    }
}