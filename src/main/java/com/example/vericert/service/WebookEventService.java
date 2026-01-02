package com.example.vericert.service;

import com.example.vericert.domain.ProcessedWebhookEvent;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.exception.DuplicateWebhookEventException;
import com.example.vericert.repo.WebhookEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebookEventService {
    private final WebhookEventRepository repo;
    public WebookEventService(WebhookEventRepository repo){
        this.repo = repo;
    }

    @Transactional
    public void markProcessed(BillingProvider provider, String eventId, String eventType) throws DuplicateWebhookEventException {
        try {
            ProcessedWebhookEvent ev = new ProcessedWebhookEvent();
            ev.setProvider(provider.name());
            ev.setEventId(eventId);
            ev.setEventType(eventType);
            repo.save(ev);
            repo.flush(); // IMPORTANTISSIMO: forza l’INSERT ora, così l’eccezione scatta qui
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateWebhookEventException(ex);
        }
    }
}
