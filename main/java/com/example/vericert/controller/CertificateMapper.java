package com.example.vericert.controller;

import com.example.vericert.dto.CertificateDto;

public final class CertificateMapper {
    private CertificateMapper() {}
    public static CertificateDto toDto(com.example.vericert.domain.Certificate t) {
        return new CertificateDto(
                t.getId(),
                t.getSerial(),
                t.getOwnerName(),
                t.getPdfUrl(),
                t.getStatus()
        );
    }
}