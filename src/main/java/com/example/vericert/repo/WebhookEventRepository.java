package com.example.vericert.repo;

import com.example.vericert.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {

}
