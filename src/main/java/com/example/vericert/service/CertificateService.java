package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.util.HashUtils;
import com.example.vericert.util.QrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import static com.example.vericert.util.PdfUtil.htmlToPdf;


@Service
public class CertificateService {
    @Value("${vericert.base-url}") String baseUrl;
    @Value("${vericert.storage.local-path}") String storagePath;
    @Value("${vericert.public-base-url:/files/certificates}") String publicBaseUrl;
    @Value("${vericert.public-base-url-verify}") String publicBaseUrlVerify;

    private final CertificateRepository certRepo;
    private final VerificationTokenRepository tokRepo;
    private final TemplateService templateService;
    private final TenantSettingsService tenantSettingsService; // usa l'opzione A, o cambia tipo se usi Plain
    private final VericertProps props;
    private final TemplateRepository tempRepo;
    private final UsageMeterService usageMeterService;
    private final QrSignerOkp qrSigner;
    private final StorageUsageService storage;
    private final CertificateStorageService certificateStorageService;
    private final PlanEnforcementService planEnforcementService;
    private final TenantSigningKeyService tenantEnsureKeyService;
    private final AesGcmCrypto crypto;
    private final PdfSigningService pdfSigningService;

    public CertificateService(CertificateRepository certRepo,
                              VerificationTokenRepository tokRepo,
                              TemplateService templateService,
                              TenantSettingsService tenantSettingsService,
                              VericertProps props,
                              TemplateRepository tempRepo,
                              UsageMeterService usageMeterService,
                              QrSignerOkp qrSigner,
                              StorageUsageService storage,
                              CertificateStorageService certificateStorageService,
                              PlanEnforcementService planEnforcementService,
                              TenantSigningKeyService tenantEnsureKeyService,
                              AesGcmCrypto crypto,
                              PdfSigningService pdfSigningService) {

        this.certRepo = certRepo;
        this.tokRepo = tokRepo;
        this.templateService = templateService;
        this.tenantSettingsService = tenantSettingsService;
        this.props = props;
        this.tempRepo = tempRepo;
        this.usageMeterService = usageMeterService;
        this.qrSigner = qrSigner;
        this.storage = storage;
        this.certificateStorageService = certificateStorageService;
        this.planEnforcementService = planEnforcementService;
        this.tenantEnsureKeyService = tenantEnsureKeyService;
        this.crypto = crypto;
        this.pdfSigningService = pdfSigningService;
    }

    @Transactional
    public Certificate issue(Long templateId,
                             Map<String, Object> vars,
                             String ownerName,
                             String ownerEmail,
                             Tenant tenant) throws Exception {



        try {
            // 1) controllo piano
            planEnforcementService.checkCanIssueDocuments(tenant.getId());
            planEnforcementService.checkCanStorePdf(tenant.getId(), BigDecimal.valueOf(0));
            // 1) Precondizioni/base
            String serial = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            String code = randomCode(24);
            Map<String, Object> sysVars = tenantSettingsService.buildBaseSysVarsForTenant(tenant.getId());
            String verifyUrl = props.getPublicBaseUrlVerify() + "/vui/" + code;
            byte[] qr = QrUtil.png(verifyUrl, 300);
            String qrBase64 = Base64.getEncoder().encodeToString(qr);
            sysVars.put("serial", serial);
            sysVars.put("code", code);
            sysVars.put("verifyUrl", verifyUrl);
            sysVars.put("qrBase64", qrBase64);
            sysVars.put("issuedAt", Instant.now());   // o formattato in stringa
            sysVars.put("tenantName", tenant.getName());
            ObjectMapper om = new ObjectMapper();
            String json = om.writeValueAsString(vars);   // -> "{\"nome\":\"Mario\",\"eta\":30}"
            // 1) Render HTML e genera PDF
            //    (se vuoi storicizzare le vars usate, salvale in Template o meglio in una tabella "certificate_data")
            String html = templateService.renderHtml(templateId, vars, sysVars);
            //TemplateHtmlSanitizer.POLICY.sanitize(html);
            byte[] pdf = htmlToPdf(html); // tua util
            SigningKeyEntity sk = tenantEnsureKeyService.ensureTenantKey(tenant.getId(), tenant.getName());
            String p12Password = crypto.decryptFromBase64(sk.getP12PasswordEnc());
            byte[] signedPdf = pdfSigningService.signPdf(pdf, sk.getP12Blob(), p12Password);
            String kid = sk.getKid();
            //Controllo che la capienza in MB non venga superata.
            long bytes = signedPdf.length;
            BigDecimal mb = BigDecimal.valueOf(bytes / 1_000_000.0);
            planEnforcementService.checkCanStorePdf(tenant.getId(), mb);
            // Costruisci URL pubblico coerente
            String Url = savePdf(serial, signedPdf, tenant);
            String pdfUrl = "/files/" + tenant.getId().toString() + "/" + serial + ".pdf";
            String sha = HashUtils.base64UrlSha256(signedPdf);
            // 3) Prepara Certificate (senza id ancora) e salva
            Certificate c = new Certificate();
            c.setTenant(tenant);
            c.setSerial(serial);
            c.setPdfUrl(pdfUrl);
            c.setSha256(sha);
            c.setOwnerName(ownerName);
            c.setOwnerEmail(ownerEmail);
            c.setUserVarsJson(json);
            c.setKid(kid);
            c = certRepo.save(c);  // ora hai c.getId() (certId)
            // 4) Token di verifica (JWS compatto nel QR)
            long now = Instant.now().getEpochSecond();
            long exp = now + 31536000;                 // es. +1 anno
            Long tenantId = tenant.getId();
            Long certId = c.getId();
            Path priv = Path.of("keys/ed25519-private.pem");
            Path pub = Path.of("keys/ed25519-public.pem");
            QrSignerOkp signer = new QrSignerOkp(priv, pub, kid);
            Map<String, Object> payloads = Map.of(
                    "tenantId", tenantId,
                    "certId", certId,
                    "sha256", sha,
                    "iat", System.currentTimeMillis() / 1000,
                    "exp", (System.currentTimeMillis() / 1000) + 31536000,
                    "jti", java.util.UUID.randomUUID().toString()
            );
            String compactJws = signer.sign(payloads);
            // 5) Salva VerificationToken in DB
            VerificationToken t = new VerificationToken();
            t.setCompactJws(compactJws);
            t.setCertificateId(certId);
            t.setCode(code);                     // se usi anche un codice human-friendly
            t.setKid(kid);           // ✅ NON publicBaseUrl: serve il KID della chiave di firma
            t.setJti(extractJti(compactJws));
            t.setCreatedAt(Instant.ofEpochSecond(now));
            t.setExpiresAt(Instant.ofEpochSecond(exp));
            t.setSha256Cached(sha);             // utile per verifiche veloci
            tokRepo.save(t);
            usageMeterService.incrementDocumentsGenerated(tenantId, signedPdf.length); // aggiorna anche storage Mb se abbiamo messo il refresh dentro
            return c;
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public String savePdf(String serial, byte[] pdf, Tenant tenant) {
        try {
            Path baseDir = Paths.get(props.getStorageLocalPath(), tenant.getId().toString());
            Files.createDirectories(baseDir);
            if (!Files.isWritable(baseDir)) {
                throw new IOException("Storage directory is not writable: " + baseDir.toAbsolutePath());
            }
            Path p = baseDir.resolve(serial + ".pdf");
            Files.write(p, pdf);
            // Costruisci URL pubblico coerente
            String publicUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl + serial + ".pdf"
                    : publicBaseUrl + "/" + serial + ".pdf";
            return publicUrl;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }






    private String extractJti(String compactJws) {
        try {
            var j = JWSObject.parse(compactJws);
            return (String) j.getPayload().toJSONObject().get("jti");
        } catch (Exception e) { return null; }
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

    public void unlockOrIssue() {

    }
    @Transactional
    public void deleteCertificate(Long id) throws IOException{
        try
        {
            tokRepo.deleteByCertificateId(id);
            certRepo.deleteById(id);
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}