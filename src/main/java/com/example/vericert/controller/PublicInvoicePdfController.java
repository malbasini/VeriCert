package com.example.vericert.controller;

import com.example.vericert.domain.Invoice;
import com.example.vericert.repo.InvoiceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v/invoices")
public class PublicInvoicePdfController {

    private final InvoiceRepository invoiceRepo;

    public PublicInvoicePdfController(InvoiceRepository invoiceRepo) {
        this.invoiceRepo = invoiceRepo;
    }

    @GetMapping("/{publicCode}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String publicCode) {
        Invoice inv = invoiceRepo.findByPublicCode(publicCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fattura non trovata"));

        if (inv.getPdfBlob() == null || inv.getPdfBlob().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String filename = (inv.getNumberDisplay() != null ? inv.getNumberDisplay() : inv.getPublicCode());
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(inv.getPdfBlob());
    }
}
