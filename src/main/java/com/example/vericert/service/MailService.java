package com.example.vericert.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}") private boolean enabled;
    @Value("${app.mail.from:support@app.vercert.org}") private String from;
    @Value("${app.mail.support:support@app.vercert.org}") private String support;

    public MailService(JavaMailSender mailSender) { this.mailSender = mailSender; }

    public void sendToSupport(String subject, String html) {
        sendHtml(support, subject, html);
    }

    public void sendHtml(String to, String subject, String html) {
        if (!enabled) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }
    public void sendToSupportFromUser(String subject, String html, String replyToEmail) {
        if (!enabled) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(from);
            h.setTo(support);
            h.setSubject(subject);
            h.setText(html, true);

            if (replyToEmail != null && !replyToEmail.isBlank()) {
                h.setReplyTo(replyToEmail);
            }

            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }
}
