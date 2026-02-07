package com.example.vericert.service;

import com.example.vericert.dto.ContactFormDto;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;

    @Value("${vercert.contact.to}")
    private String supportTo;

    @Value("${vercert.contact.from}")
    private String fromEmail;

    @Value("${vercert.contact.fromName:Vercert Support}")
    private String fromName;

    public ContactEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactRequest(ContactFormDto dto, String clientIp, String userAgent) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());

            // FROM: deve essere un mittente verificato in Brevo (dominio verificato + sender configurato)
            helper.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));

            // TO: la tua mailbox Namecheap
            helper.setTo(supportTo);

            // REPLY-TO: l’utente del form (così “Rispondi” va a lui)
            helper.setReplyTo(dto.getEmail());

            // Subject: pulito e informativo
            String subject = (dto.getSubject() != null && !dto.getSubject().isBlank())
                    ? dto.getSubject().trim()
                    : "Richiesta dal form contatti";
            helper.setSubject("[Vercert] " + subject + " — " + safe(dto.getName()));

            // Corpo testo (semplice, leggibile, zero HTML = meno problemi spam)
            String body = ""
                    + "Nuova richiesta dal form contatti\n"
                    + "---------------------------------\n"
                    + "Nome: " + safe(dto.getName()) + "\n"
                    + "Email: " + safe(dto.getEmail()) + "\n"
                    + "Timestamp: " + Instant.now() + "\n"
                    + "IP: " + (clientIp != null ? clientIp : "-") + "\n"
                    + "User-Agent: " + (userAgent != null ? userAgent : "-") + "\n"
                    + "\n"
                    + "Messaggio:\n"
                    + safe(dto.getMessage()) + "\n";

            helper.setText(body);

            // Extra headers utili (opzionali)
            msg.setHeader("X-Entity-Ref-ID", "vercert-contact");
            msg.setHeader("X-Auto-Response-Suppress", "All"); // evita auto-reply loop in certi client
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Invio email contatti fallito: " + e.getMessage(), e);
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        // elimina CR/LF per prevenire header injection, mantiene il testo pulito
        return s.replace("\r", "").replace("\n", "\n").trim();
    }
}
