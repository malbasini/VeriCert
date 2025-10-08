package com.example.vericert.controller;

import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.VerificationTokenRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PublicVerificationHtmlController {

    private final VerificationTokenRepository tokenRepo;

    public PublicVerificationHtmlController(VerificationTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @GetMapping(value="/v/{code}", produces= MediaType.TEXT_HTML_VALUE)
    public String verifyPage(@PathVariable String code, Model model) {
        var view = tokenRepo.findViewByCode(code).orElse(null);
        if (view == null) {
            model.addAttribute("valid", false);
            model.addAttribute("reason", "Codice non trovato");
            return "verification/result";
        }
        if (Boolean.TRUE.equals(view.getRevokedAt())) {
            model.addAttribute("valid", false);
            model.addAttribute("reason", "Certificato revocato");
        } else {
            model.addAttribute("valid", true);
        }
        model.addAttribute("code", view.getCode());
        model.addAttribute("serial", view.getSerial());
        model.addAttribute("ownerName", view.getOwnerName());
        model.addAttribute("courseName", view.getCourseCode());
        model.addAttribute("issuedAt", view.getIssuedAt());
        // Se vuoi mostrare info tenant:
        model.addAttribute("tenantId", view.getTenantId());
        return "verification/result";
    }
}
