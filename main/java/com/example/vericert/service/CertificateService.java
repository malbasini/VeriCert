package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.*;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.util.HashUtil;
import com.example.vericert.util.PdfUtil;
import com.example.vericert.util.QrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class CertificateService {
    @Value("${vericert.base-url}") String baseUrl;
    @Value("${vericert.storage.local-path}") String storagePath;
    @Value("${vericert.storage.public-base:/files/certificates}") String publicBaseUrl;


    private final CertificateRepository certRepo;
    private final VerificationTokenRepository tokRepo;
    private final TemplateService templateService;
    private final TenantSettingsService tenantSettingsService; // usa l'opzione A, o cambia tipo se usi Plain
    private final VericertProps props;
    private final TemplateRepository tempRepo;

    public CertificateService(CertificateRepository certRepo,
                              VerificationTokenRepository tokRepo,
                              TemplateService templateService,
                              TenantSettingsService tenantSettingsService,
                              VericertProps props,
                              TemplateRepository tempRepo){

        this.certRepo = certRepo;
        this.tokRepo = tokRepo;
        this.templateService = templateService;
        this.tenantSettingsService = tenantSettingsService;
        this.props = props;
        this.tempRepo = tempRepo;
    }

    @Transactional
    public Certificate issue(Long templateId, Map<String,Object> vars, String ownerName, String ownerEmail, Tenant tenant) throws Exception {
        Template template = tempRepo.findById(templateId).orElseThrow();
        String serial = UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();
        String code = randomCode(24);
        String verifyUrl = props.getPublicBaseUrl() + "/v/" + code;
        byte[] qr = QrUtil.png(verifyUrl, 300);
        String qrBase64 = Base64.getEncoder().encodeToString(qr);
        Map<String,Object> sysVars = tenantSettingsService.buildBaseSysVarsForTenant(tenant.getId());
        sysVars.put("serial", serial);
        sysVars.put("code", code);
        sysVars.put("verifyUrl", verifyUrl);
        sysVars.put("qrBase64", qrBase64);
        sysVars.put("issuedAt", Instant.now());   // o formattato in stringa
        sysVars.put("tenantName", tenant.getName());
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(vars);   // -> "{\"nome\":\"Mario\",\"eta\":30}"
        template.setUserVarJson(json);
        tempRepo.save(template);
        String html = templateService.renderHtml(templateId, vars, sysVars);
        byte[] pdf = PdfUtil.htmlToPdf(html);
        String sha = HashUtil.sha256Hex(pdf);
        Files.createDirectories(Paths.get(storagePath));
        String pdfUrl = savePdf(serial, pdf);
        Certificate c = new Certificate();
        c.setTenant(tenant);
        c.setSerial(serial);
        c.setOwnerName(ownerName);
        c.setOwnerEmail(ownerEmail);
        c.setPdfUrl(pdfUrl);
        c.setSha256(sha);
        c.setTenant(tenant);
        c = certRepo.save(c);
        VerificationToken t = new VerificationToken();
        t.setCode(code);
        t.setCertificateId(c.getId());
        tokRepo.save(t);
        return c;
    }
    private String savePdf(String serial, byte[] pdf) {
        try {
            // Normalizza il path: consenti sia "storage" che "storage/certificates"
            Path baseDir = Paths.get(storagePath).normalize();
            // Se l'ultima parte non è "certificates", usa una sottodirectory "certificates"
            Path targetDir = baseDir.getFileName() != null && baseDir.getFileName().toString().equals("certificates")
                    ? baseDir
                    : baseDir.resolve("certificates");
            Files.createDirectories(targetDir);
            if (!Files.isWritable(targetDir)) {
                throw new IOException("Storage directory is not writable: " + targetDir.toAbsolutePath());
            }
            Path p = targetDir.resolve(serial + ".pdf");
            Files.write(p, pdf);
            // Costruisci URL pubblico coerente
            String publicUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl + serial + ".pdf"
                    : publicBaseUrl + "/" + serial + ".pdf";
            return publicUrl;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public static String randomCode(int len){
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for(int i=0;i<len;i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
    @Transactional
    public void revoke(Long certId, String reason, String actor) {
        Certificate c = certRepo.findById(certId).orElseThrow();
        c.setStatus(c.setStatus(Stato.REVOKED));
        c.setRevokedReason(reason);
        c.setRevokedAt(Instant.now());
        audit(actor,"REVOKE","certificate", certId.toString(), Map.of("reason", reason));
    }
    private void audit(String actor, String action, String entity, String entityId, Map<String,Object> payload) {
        // puoi persistere in audit_log (omesso per brevità) o loggare in JSON
    }
    public Page<Certificate> listForTenant(Long tenantId, String q, Stato status, Pageable pageable) {
        return certRepo.search(tenantId, q, status, pageable);
    }
    public Certificate getForTenant(Long id) {
        return certRepo.findById(id).orElseThrow();
    }
    @Transactional
    public void revoke(Long tenantId, Long id, String reason, String byUser) {
        Certificate c = getForTenant(id);
        if (c.getStatus() == Stato.REVOKED) return;
        c.setStatus(Stato.REVOKED);
        certRepo.save(c);
        // opzionale: audit
        // auditLog.infoRevoke(tenantId, id, reason, byUser);
        // opzionale: invalidare token verifica (se vuoi far fallire la verifica)
    }
}