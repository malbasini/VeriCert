package com.example.vericert.service;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.util.HashUtil;
import com.example.vericert.util.PdfUtil;
import com.example.vericert.util.QrUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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
    private final TenantRepository tenantRepo; // usa l'opzione A, o cambia tipo se usi Plain


    public CertificateService(CertificateRepository certRepo,
                              VerificationTokenRepository tokRepo,
                              TemplateService templateService,
                              TenantRepository tenantRepo){

        this.certRepo = certRepo;
        this.tokRepo = tokRepo;
        this.templateService = templateService;
        this.tenantRepo = tenantRepo;
    }

    @Transactional
    public Certificate issue(Long templateId, Map<String,Object> vars, String ownerName, String ownerEmail, String courseCode) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        Long tenantId = tenantRepo.findByName(tenantName).getId();
        String serial = UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();
        String code = randomCode(24);
        String verifyUrl = baseUrl + "/v/" + code;
        byte[] qr = QrUtil.png(verifyUrl, 300);
        String qrBase64 = Base64.getEncoder().encodeToString(qr);
        Map<String,Object> sys = new HashMap<>();
        sys.put("SERIAL", serial);
        sys.put("VERIFY_URL", verifyUrl);
        sys.put("QR_BASE64", qrBase64);
        vars.put("ownerName", ownerName);
        vars.put("ownerEmail", ownerEmail);
        vars.put("courseCode", courseCode);
        String html = templateService.renderHtml(templateId, vars, sys);
        byte[] pdf = PdfUtil.htmlToPdf(html);
        String sha = HashUtil.sha256Hex(pdf);
        Files.createDirectories(Paths.get(storagePath));
        String pdfUrl = savePdf(serial, pdf);

        Certificate c = new Certificate();
        c.setTenantId(tenantId);
        c.setSerial(serial);
        c.setOwnerName(ownerName);
        c.setOwnerEmail(ownerEmail);
        c.setCourseCode(courseCode);
        c.setPdfUrl(pdfUrl);
        c.setSha256(sha);
        Tenant tenant = tenantRepo.getById(tenantId);
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
    private static String randomCode(int len){
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for(int i=0;i<len;i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
    @Transactional
    public void revoke(Long certId, String reason, String actor) {
        Certificate c = certRepo.findById(certId).orElseThrow();
        c.setStatus(Certificate.Status.REVOKED);
        c.setRevokedReason(reason);
        c.setRevokedAt(Instant.now());
        audit(actor,"REVOKE","certificate", certId.toString(), Map.of("reason", reason));
    }
    private void audit(String actor, String action, String entity, String entityId, Map<String,Object> payload) {
        // puoi persistere in audit_log (omesso per brevità) o loggare in JSON
    }
}