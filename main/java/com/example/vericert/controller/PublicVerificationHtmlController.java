package com.example.vericert.controller;

import com.example.vericert.dto.VerificationOutcome;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.CertificateStorageService;
import com.example.vericert.service.QrVerificationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class PublicVerificationHtmlController {

    private final VerificationTokenRepository tokenRepo;
    private final QrVerificationService service;
    private final CertificateStorageService storageService;

    public PublicVerificationHtmlController(VerificationTokenRepository tokenRepo,
                                            QrVerificationService service,
                                            CertificateStorageService storageService) {
        this.tokenRepo = tokenRepo;
        this.service = service;
        this.storageService = storageService;

    }

    @GetMapping(value = "/v/{code}", produces = MediaType.TEXT_HTML_VALUE)
    public String verifyPage(@PathVariable String code, Model model) throws IOException {

        var viewOpt = tokenRepo.findViewByCode(code);
        if (viewOpt.isEmpty()) {
            model.addAttribute("valid", false);
            model.addAttribute("reason", "Codice non trovato");
            return "verification/result";
        }
        var view = viewOpt.get();

        // 1) Usa il token EMESSO (immutabile)
        String compactJws = view.getCompactJws();   // <-- recuperato da DB; NON rigenerare

        // 2) Carica i byte REALI del PDF emesso (non rigenerarlo da HTML!)
        byte[] pdfBytes = storageService.loadPdfBytes(view.getTenantId(),view.getSerial());

        // 3) Verifica firma + scadenza + revoca + integrità
        VerificationOutcome v = service.verify(
                view.getTenantId(),
                compactJws,
                QrVerificationService.Source.WEB
        );

        // 4) Popola il modello
        model.addAttribute("code",       view.getCode());
        model.addAttribute("serial",     view.getSerial());
        model.addAttribute("ownerName",  view.getOwnerName());
        model.addAttribute("issuedAt",   view.getIssuedAt());
        model.addAttribute("tenantId",   view.getTenantId());
        model.addAttribute("valid",      v.ok());
        model.addAttribute("reason",     v.ok() ? null : v.reason());

        return "verification/result";
    }

}
