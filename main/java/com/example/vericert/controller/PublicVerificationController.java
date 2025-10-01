package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@Controller
public class PublicVerificationController {

    private final VerificationTokenRepository tokRepo;
    private final CertificateRepository certRepo;

    public PublicVerificationController(VerificationTokenRepository tokRepo, CertificateRepository certRepo) {
        this.tokRepo = tokRepo;
        this.certRepo = certRepo;
    }

    @GetMapping(value="/v/{code}")
    public String verifyHtml(@PathVariable(name="code") String code, Model model){
        var map = verify(code);
        model.addAttribute("r", map);
        return "verification/result";
    }


    @GetMapping(value="/v/{code}.json", produces= MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, ? extends Serializable> verifyJson(@PathVariable String code){ return verify(code); }


    private Map<String, ? extends Serializable> verify(String code){
        return tokRepo.findById(code).map(t -> {
            Optional<Certificate> certificate = certRepo.findById(t.getCertificateId());
            Certificate c = certificate.get();
            if(c.getRevokedReason() == null)
                c.setRevokedReason("");
            return Map.of(
                    "status", c.getStatus().name(),
                    "serial", c.getSerial(),
                    "ownerName", c.getOwnerName(),
                    "courseCode", c.getCourseCode(),
                    "issuedAt", c.getIssuedAt(),
                    "revokedReason", c.getRevokedReason(),
                    "pdfUrl", c.getPdfUrl(),
                    "sha256", c.getSha256()
            );
        }).orElse(Map.of("status", "UNKNOWN"));
    }
}