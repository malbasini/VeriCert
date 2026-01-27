package com.example.vericert.exception;

import org.springframework.dao.DataIntegrityViolationException;

public class DuplicateWebhookEventException extends Throwable {
    public DuplicateWebhookEventException(DataIntegrityViolationException ex) {
        super(ex.getMessage());
    }
}
