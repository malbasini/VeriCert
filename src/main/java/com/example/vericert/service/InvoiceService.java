package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.*;
import com.example.vericert.dto.InvoiceTotals;
import com.example.vericert.dto.UpsertInvoiceReq;
import com.example.vericert.enumerazioni.InvoiceStatus;
import com.example.vericert.repo.*;
import com.example.vericert.util.PublicCodeGenerator;
import com.example.vericert.util.QrUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
public class InvoiceService {
    @Value("${vericert.base-url}") String baseUrl;
    @Value("${vericert.storage.local-path}") String storagePath;
    @Value("${vericert.public-base-url:/files/certificates}") String publicBaseUrl;
    @Value("${vericert.public-base-url-verify}") String publicBaseUrlVerify;


    private final PlanEnforcementService planEnforcementService;
    private final UsageMeterService usageMeterService;
    private final TenantSettingsService tenantSettingsService;
    private final UserRepository userRepo;
    private final InvoiceCustomerRepository invoiceCustomerRepo;
    private final PdfRenderService pdfRenderService;
    private final TemplateRepository templateRepo;
    private final InvoiceLineRepository lineRepo;
    private final InvoiceRepository invoiceRepo;
    private final VericertProps props;
    private final InvoiceTemplateService invoiceTemplateService;
    private final TenantProfileRepository tenantProfileRepo;
    private final org.thymeleaf.TemplateEngine templateEngine; // Cambio tipo qui// tua interfaccia (vedi sotto)
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    private final TenantSigningKeyService tenantEnsureKeyService;
    private final AesGcmCrypto crypto;
    private final PdfSigningService pdfSigningService;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          TenantProfileRepository tenantProfileRepo,
                          InvoiceLineRepository lineRepo,
                          TemplateRepository templateRepo,
                          InvoiceTemplateService invoiceTemplateService,
                          PdfRenderService pdfRenderService,
                          InvoiceCustomerRepository invoiceCustomerRepo,
                          UserRepository userRepo,
                          @Qualifier("dbTemplateEngine") org.thymeleaf.TemplateEngine templateEngine,
                          VericertProps props,
                          TenantSettingsService tenantSettingsService,
                          UsageMeterService usageMeterService,
                          PlanEnforcementService planEnforcementService,
                          TenantSigningKeyService tenantEnsureKeyService,
                          AesGcmCrypto crypto,
                          PdfSigningService pdfSigningService) {

        this.invoiceRepo = invoiceRepo;
        this.tenantProfileRepo = tenantProfileRepo;
        this.lineRepo = lineRepo;
        this.templateRepo = templateRepo;
        this.invoiceTemplateService = invoiceTemplateService;
        this.pdfRenderService = pdfRenderService;
        this.invoiceCustomerRepo = invoiceCustomerRepo;
        this.userRepo = userRepo;
        this.templateEngine = templateEngine;
        this.props = props;
        this.tenantSettingsService = tenantSettingsService;
        this.usageMeterService = usageMeterService;
        this.planEnforcementService = planEnforcementService;
        this.tenantEnsureKeyService = tenantEnsureKeyService;
        this.crypto = crypto;
        this.pdfSigningService = pdfSigningService;
    }

    @Transactional
    public Invoice createDraft(Long tenantId, UpsertInvoiceReq req) {

        Template tpl = templateRepo.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new IllegalStateException("Nessun template attivo per tenant " + tenantId));

        if (req == null) throw new IllegalArgumentException("Richiesta nulla");
        if (req.customerName() == null || req.customerName().isBlank())
            throw new IllegalArgumentException("customerName obbligatorio");
        if (req.lines() == null || req.lines().isEmpty())
            throw new IllegalArgumentException("Inserisci almeno una riga");

        Invoice inv = new Invoice();
        inv.setTenantId(tenantId);
        inv.setStatus(InvoiceStatus.DRAFT);
        inv.setVatRate(req.vatRate() != null ? req.vatRate() : 22);
        inv.setCustomerName(req.customerName().trim());
        inv.setCustomerVat(req.customerVat());
        inv.setCustomerEmail(req.customerEmail());
        inv.setTemplateId(tpl.getId());
        inv.setDescription(req.description());
        inv.setIssuedAt(Instant.now());
        inv.setCustomerAddressLine1(req.customerAddressLine1());
        inv.setCustomerAddressLine2(req.customerAddressLine2());
        inv.setCustomerCity(req.customerCity());
        inv.setCustomerCountry(req.customerCountry());
        inv.setCustomerPostalCode(req.customerPostalCode());
        inv.setCustomerProvince(req.customerProvince());
        inv.setCustomerCity(req.customerCity());
        inv.setCustomerPec(req.customerPec());
        inv.setCustomerSdi(req.customerSdi());
        if (req.lines() != null) {
            for (var l : req.lines()) {

                if (l == null) continue;

                if (l.description() == null || l.description().isBlank())
                    throw new IllegalArgumentException("Descrizione riga obbligatoria");
                if (l.qty() == null || l.qty() <= 0)
                    throw new IllegalArgumentException("qty deve essere >= 1");
                if (l.unitPriceMinor() == null || l.unitPriceMinor() < 0)
                    throw new IllegalArgumentException("unitPriceMinor non valido");

                InvoiceLine line = new InvoiceLine();
                line.setDescription(l.description());
                line.setQty(l.qty() != null ? l.qty() : 1);
                line.setUnitPriceMinor(l.unitPriceMinor() != null ? l.unitPriceMinor() : 0L);
                inv.addLine(line);
            }
        }

        computeTotals(inv);
        // public_code: rigenera finché non è univoco
        for (int i = 0; i < 10; i++) {
            inv.setPublicCode(PublicCodeGenerator.newInvoiceCode()); // INV-8F3K2P
            try {
                inv.setInvoiceSave(true);
                Invoice invoice = invoiceRepo.save(inv);
                Long maxNumberSequence = invoiceRepo.maxSeqForYear(tenantId,invoice.getIssueYear());
                maxNumberSequence +=1;
                invoice.setNumberSeq(maxNumberSequence);
                invoiceRepo.save(invoice);
                return invoice;
            } catch (DataIntegrityViolationException dup) {
                // collisione public_code
            }
        }
        throw new IllegalStateException("Impossibile generare public_code univoco");
    }
    @Transactional
    public Invoice issue(Long tenantId, Long invoiceId) throws Exception {
        try {
            // 1) controllo piano
            planEnforcementService.checkCanIssueDocuments(tenantId);
            planEnforcementService.checkCanStorePdf(tenantId, BigDecimal.valueOf(0));

            Invoice inv = invoiceRepo.findById(invoiceId).orElseThrow(() -> new IllegalStateException("Dati di fatturazione mancanti (Invoice)"));
            ;
            if (!tenantId.equals(inv.getTenantId())) throw new SecurityException("Forbidden");
            if (!"DRAFT".equals(inv.getStatus().name())) throw new IllegalStateException("Fattura non modificabile");

            TenantProfile supplier = tenantProfileRepo.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Dati di fatturazione mancanti (tenant_profile)"));


            if (supplier.getCompanyName().isEmpty() || supplier.getVatNumber().isEmpty()) {
                throw new IllegalStateException("Completa Ragione Sociale e P.IVA prima di emettere la fattura.");
            }

            List<InvoiceLine> lines = lineRepo.findByInvoiceIdOrderBySortOrderAsc(invoiceId);
            if (lines.isEmpty()) throw new IllegalStateException("La fattura non ha righe.");

            // Totali
            long net = 0, vat = 0, gross = 0;
            for (InvoiceLine l : lines) {
                net += l.getNetMinor();
                vat += l.getVatMinor();
                gross += l.getGrossMinor();
            }
            inv.setNetTotalMinor(net);
            inv.setVatTotalMinor(vat);
            inv.setGrossTotalMinor(gross);

            // codice fattura + data
            inv.setInvoiceCode(generateInvoiceCodeUnique()); // vedi nota sotto
            Template tpl = templateRepo.findById(inv.getTemplateId())
                    .orElseThrow(() -> new IllegalStateException("Template fattura non trovato"));

            String verifyUrl = props.getPublicBaseUrlVerify() + "/vf/invoices/" + inv.getPublicCode();
            byte[] qr = QrUtil.png(verifyUrl, 300);
            String qrBase64 = Base64.getEncoder().encodeToString(qr);
            Map<String, Object> sysVars = tenantSettingsService.buildBaseSysVarsForTenant(tenantId);
            // MODEL Thymeleaf
            Map<String, Object> model = new HashMap<>();
            model.putAll(sysVars);
            model.put("verifyUrl", verifyUrl);
            model.put("qrBase64", qrBase64);
            model.put("supplier", supplier);
            model.put("invoice", inv);
            model.put("lines", lines);
            model.put("totals", new InvoiceTotals(net, vat, gross));
            // Render HTML
            String html = invoiceTemplateService.renderInvoiceHtml(tpl.getHtml(), model);
            // Render PDF
            byte[] pdf = pdfRenderService.render(html);
            Tenant tenant = tpl.getTenant();
            String serial = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            SigningKeyEntity sk = tenantEnsureKeyService.ensureTenantKey(tenant.getId(), tenant.getName());
            String p12Password = crypto.decryptFromBase64(sk.getP12PasswordEnc());
            byte[] signedPdf = pdfSigningService.signPdf(pdf, sk.getP12Blob(), p12Password);
            String kid = sk.getKid();
            String Url = savePdf(serial, signedPdf, tenant);
            String pdfUrl = "/files/" + tenant.getId().toString() + "/" + serial + ".pdf";
            inv.setPdfUrl(pdfUrl);
            inv.setKid(kid);
            inv.setSerial(serial);
            inv.setPdfBlob(signedPdf);
            inv.setPdfSha256(sha256Hex(signedPdf));
            //Controllo che la capienza in MB non venga superata.
            long bytes = signedPdf.length;
            BigDecimal mb = BigDecimal.valueOf(bytes / 1_000_000.0);
            planEnforcementService.checkCanStorePdf(tenant.getId(), mb);
            usageMeterService.incrementDocumentsGenerated(tenantId, signedPdf.length);
            inv.setIssuedAt(Instant.now());
            inv.setStatus(InvoiceStatus.valueOf("ISSUED"));
            return invoiceRepo.save(inv);
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private String generateInvoiceCodeUnique() {
        for (int i = 0; i < 10; i++) {
            String code = PublicCodeGenerator.newInvoiceCode(); // INV-8F3K2P
            if (invoiceRepo.findByInvoiceCode(code).isEmpty()) return code;
        }
        throw new IllegalStateException("Impossibile generare invoice_code univoco");
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
    @Transactional
    protected void computeTotals(Invoice inv) {
        long net = 0;
        long vat = 0;
        long gross = 0;

        int vatRate = (inv.getVatRate() != null ? inv.getVatRate() : 22);

        for (InvoiceLine l : inv.getLines()) {

            Integer qtyObj = l.getQty();
            Long unitObj = l.getUnitPriceMinor();

            if (qtyObj == null || qtyObj <= 0) {
                throw new IllegalArgumentException("qty non valida nella riga");
            }
            if (unitObj == null || unitObj < 0) {
                throw new IllegalArgumentException("unitPriceMinor non valido nella riga");
            }

            int qty = qtyObj;
            long unit = unitObj;

            long lineNet = Math.multiplyExact(unit, (long) qty);

            // IVA half-up in integer math
            long lineVat = (Math.multiplyExact(lineNet, vatRate) + 50) / 100;

            long lineGross = Math.addExact(lineNet, lineVat);

            l.setVatRate(vatRate);
            l.setNetMinor(lineNet);
            l.setVatMinor(lineVat);
            l.setGrossMinor(lineGross);

            net = Math.addExact(net, lineNet);
            vat = Math.addExact(vat, lineVat);
            gross = Math.addExact(gross, lineGross);
        }

        inv.setNetTotalMinor(net);
        inv.setVatTotalMinor(vat);
        inv.setGrossTotalMinor(gross);
    }
    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long netFromGross(long grossMinor, double vatRate){
        // gross = net * (1 + vatRate)  -> net = gross / 1.22
        return Math.round(grossMinor / (1.0 + vatRate));
    }

    private Invoice requireOwnedDraft(Long tenantId, Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId).orElseThrow();
        if (!tenantId.equals(inv.getTenantId())) throw new SecurityException("Forbidden");
        if (!"DRAFT".equals(inv.getStatus())) throw new IllegalStateException("Fattura non modificabile");
        return inv;
    }
    private long vatMinor(long netMinor, double vatRate){
        return Math.round(netMinor * vatRate);
    }

    private void recalcTotals(Invoice inv) {
        List<InvoiceLine> lines = lineRepo.findByInvoiceIdOrderBySortOrderAsc(inv.getId());
        long net = 0, vat = 0, gross = 0;
        for (InvoiceLine l : lines) {
            net += l.getNetMinor();
            vat += l.getVatMinor();
            gross += l.getGrossMinor();
        }
        inv.setNetTotalMinor(net);
        inv.setVatTotalMinor(vat);
        inv.setGrossTotalMinor(gross);
    }
    public Invoice getOwned(Long tenantId, Long id) {
        return invoiceRepo.findByTenantIdAndId(tenantId, id).orElseThrow();
    }
    @Transactional
    public Invoice updateDraft(Long tenantId, Long id, UpsertInvoiceReq req) {
        Invoice inv = invoiceRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice non trovata"));

        if (req == null) throw new IllegalArgumentException("Richiesta nulla");
        if (req.customerName() == null || req.customerName().isBlank())
            throw new IllegalArgumentException("customerName obbligatorio");
        if (req.lines() == null || req.lines().isEmpty())
            throw new IllegalArgumentException("Inserisci almeno una riga");

        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Solo le DRAFT sono modificabili");
        }

        inv.setCustomerName(req.customerName());
        inv.setCustomerVat(req.customerVat());
        inv.setCustomerEmail(req.customerEmail());
        inv.setVatRate(req.vatRate() != null ? req.vatRate() : 22);
        if (req.description() != null) inv.setDescription(req.description());
        inv.setCustomerAddressLine1(req.customerAddressLine1());
        inv.setCustomerAddressLine2(req.customerAddressLine2());
        inv.setCustomerCity(req.customerCity());
        inv.setCustomerCountry(req.customerCountry());
        inv.setCustomerPostalCode(req.customerPostalCode());
        inv.setCustomerProvince(req.customerProvince());
        inv.setCustomerCity(req.customerCity());
        inv.setCustomerPec(req.customerPec());
        inv.setCustomerSdi(req.customerSdi());

        if (req.templateId() != null) {
            inv.setTemplateId(req.templateId());
        }
        inv.setTenantId(tenantId);
        inv.setStatus(InvoiceStatus.DRAFT);

        // replace lines//
        inv.clearLines();
        if (req.lines() != null) {
            for (var l : req.lines()) {

                if (l == null) continue;

                if (l.description() == null || l.description().isBlank())
                    throw new IllegalArgumentException("Descrizione riga obbligatoria");
                if (l.qty() == null || l.qty() <= 0)
                    throw new IllegalArgumentException("qty deve essere >= 1");
                if (l.unitPriceMinor() == null || l.unitPriceMinor() < 0)
                    throw new IllegalArgumentException("unitPriceMinor non valido");

                InvoiceLine line = new InvoiceLine();
                line.setDescription(l.description());
                line.setQty(l.qty() != null ? l.qty() : 1);
                line.setUnitPriceMinor(l.unitPriceMinor() != null ? l.unitPriceMinor() : 0L);
                inv.addLine(line);
            }
        }

        computeTotals(inv);
        return invoiceRepo.save(inv);
    }

}
