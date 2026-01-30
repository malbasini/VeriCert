package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.Invoice;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.TenantProfile;
import com.example.vericert.dto.InvoiceLineReq;
import com.example.vericert.dto.InvoiceLineView;
import com.example.vericert.dto.InvoicePreviewReq;
import com.example.vericert.repo.TenantProfileRepository;
import com.example.vericert.util.PublicCodeGenerator;
import com.example.vericert.util.QrUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static com.example.vericert.util.PdfUtil.htmlToPdf;

@Service
public class InvoicePreviewService {

    private final TenantProfileRepository tenantProfileRepo;
    private final InvoiceTemplateService invoiceTemplateService;
    private final TenantSettingsService tenantSettingsService;
    private final VericertProps props;

    public InvoicePreviewService(TenantProfileRepository tenantProfileRepo,
                                 InvoiceTemplateService invoiceTemplateService,
                                 TenantSettingsService tenantSettingsService,
                                 VericertProps props) {

        this.tenantProfileRepo = tenantProfileRepo;
        this.invoiceTemplateService = invoiceTemplateService;
        this.tenantSettingsService = tenantSettingsService;
        this.props = props;
    }
    public String renderHtml(Long tenantId, Template template, InvoicePreviewReq req) {
        Map<String, Object> model = buildModel(tenantId, req);
        // template.getHtml() contiene HTML con th:text ecc.
        return invoiceTemplateService.renderInvoiceHtml(template.getHtml(), model);
    }

    public byte[] renderPdf(Long tenantId, Template template, InvoicePreviewReq req) throws Exception {
        String html = renderHtml(tenantId, template, req);
        return htmlToPdf(html);
    }

    private Map<String, Object> buildModel(Long tenantId, InvoicePreviewReq req) {
        int vatRate = (req.vatRate() != null ? req.vatRate() : 22);
        // FORNITORE: viene da tenant_profile
        TenantProfile prof = tenantProfileRepo.findByTenantId(tenantId).orElse(null);
        List<InvoiceLineView> lines = new ArrayList<>();
        long net = 0, vat = 0, gross = 0;
        if (req.lines() == null || req.lines().isEmpty())
            throw new IllegalArgumentException("Inserisci almeno una riga");
        if (req == null) throw new IllegalArgumentException("Richiesta nulla");
        if (req.lines() != null) {
            for (InvoiceLineReq l : req.lines()) {
                if (l.description() == null || l.description().isBlank())
                    throw new IllegalArgumentException("Descrizione riga obbligatoria");
                if (l.qty() == null || l.qty() <= 0)
                    throw new IllegalArgumentException("qty deve essere >= 1");
                if (l.unitPriceMinor() == null || l.unitPriceMinor() < 0)
                    throw new IllegalArgumentException("unitPriceMinor non valido");
                int qty = (l.qty() != null ? l.qty() : 1);
                long unit = (l.unitPriceMinor() != null ? l.unitPriceMinor() : 0L);
                long lineNet = unit * qty;
                long lineVat = Math.round(lineNet * (vatRate / 100.0));
                long lineGross = lineNet + lineVat;
                lines.add(new InvoiceLineView(
                        l.description(),
                        qty,
                        unit,
                        lineNet,
                        lineVat,
                        lineGross
                ));

                net += lineNet;
                vat += lineVat;
                gross += lineGross;
            }
        }
        Map<String, Object> header = (req.header() != null ? req.header() : Map.of());
        String verifyUrl = props.getPublicBaseUrl() + "/vf/invoices/" + PublicCodeGenerator.newInvoiceCode();
        byte[] qr = QrUtil.png(verifyUrl, 300);
        String qrBase64 = Base64.getEncoder().encodeToString(qr);
        Map<String,Object> sysVars = tenantSettingsService.buildBaseSysVarsForTenant(tenantId);
        // MODEL Thymeleaf
        Map<String, Object> model = new HashMap<>();
        Invoice inv = new Invoice();
        inv.setCustomerAddressLine1(header.get("customerAddressLine1").toString());
        inv.setCustomerCity(header.get("customerCity").toString());
        inv.setCustomerPostalCode(header.get("customerPostalCode").toString());
        inv.setCustomerProvince(header.get("customerProvince").toString());
        inv.setCustomerName(header.get("customerName").toString());
        inv.setCustomerVat(header.get("customerVat").toString());
        inv.setNetTotalMinor(net);
        inv.setVatTotalMinor(vat);
        inv.setGrossTotalMinor(gross);
        inv.setInvoiceCode(PublicCodeGenerator.newInvoiceCode());
        inv.setIssuedAt(Instant.now());
        model.putAll(sysVars);
        model.put("verifyUrl", verifyUrl);
        model.put("qrBase64", qrBase64);
        model.put("supplier", prof);
        model.put("invoice", inv);
        model.put("lines", lines);
        model.put("vatRate", vatRate);
        return model;
        }

}

