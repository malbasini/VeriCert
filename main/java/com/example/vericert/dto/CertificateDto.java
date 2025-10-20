package com.example.vericert.dto;

import com.example.vericert.domain.Stato;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public record CertificateDto(

        Long id,
        String serial,
        String owner,
        String pdfUrl,
        @Enumerated(EnumType.STRING) @Column(name = "status", nullable=false)
        Stato status)
{ }
