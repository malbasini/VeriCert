package com.example.vericert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper om;

    @Value("${brevo.template.purchaseSuccess")
    private String templateId;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;
    @Value("${app.mail.from:support@app.vercert.org}")
    private String from;
    @Value("${app.mail.support:support@app.vercert.org}")
    private String support;

    public MailService(JavaMailSender mailSender,
                       ObjectMapper om) {
        this.mailSender = mailSender;
        this.om = om;
    }

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
    public void sendPurchaseSuccess(String toEmail, String subject, Map<String, Object> vars) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom("no-reply@app.vercert.org");
            helper.setSubject(subject);
            helper.setText(" ", true);
            // ✅ UNICO header corretto per template via SMTP
            Map<String, Object> sibApi = new HashMap<>();
            sibApi.put("templateId", 4);     // usa il TUO ID reale (nel log era 4)
            sibApi.put("params", vars);
            msg.setHeader("X-SIB-API", om.writeValueAsString(sibApi));
            // ❌ rimuovi questi:
            // msg.setHeader("X-Sib-TemplateId", ...);
            // msg.setHeader("X-Sib-Variables", ...);
            mailSender.send(msg);

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Brevo email send failed: {}", e.getMessage(), e);
        }

    }


    public void sendPaymentMail(String to, String subject, Map<String,Object> vars) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);

            // IMPORTANT: placeholder body per SMTP relay (Brevo sostituisce via header)
            helper.setText(" ", true);

            msg.setHeader("X-Sib-TemplateId", "4");
            msg.setHeader("X-Sib-Variables", om.writeValueAsString(vars));

            mailSender.send(msg);
        } catch (Exception e) {
            // non far fallire il webhook per colpa email
            LoggerFactory.getLogger(getClass()).warn("Brevo email send failed: {}", e.getMessage(), e);
        }
    }






}
