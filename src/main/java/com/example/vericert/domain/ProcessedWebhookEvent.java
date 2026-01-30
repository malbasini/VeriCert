package com.example.vericert.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name="processed_webhook_event")
public class ProcessedWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="provider",nullable = false)
    private String provider;
    @Column(name="event_id",nullable = false)
    private String eventId;
    @Column(name="event_type",nullable = false)
    private String eventType;

    @UpdateTimestamp
    @Column(name="received_at",nullable = false)
    private Instant receviedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getReceviedAt() {
        return receviedAt;
    }

    public void setReceviedAt(Instant receviedAt) {
        this.receviedAt = receviedAt;
    }
}
