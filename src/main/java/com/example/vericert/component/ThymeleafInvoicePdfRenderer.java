package com.example.vericert.component;

import com.example.vericert.domain.Invoice;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.TenantProfile;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantProfileRepository;
import com.example.vericert.service.InvoicePdfRenderer;
import com.example.vericert.service.ValidationService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Component
public class ThymeleafInvoicePdfRenderer implements InvoicePdfRenderer {

    private final TemplateEngine engine;
    private final TenantProfileRepository tenantProfileRepo;
    private final TemplateRepository templates;
    private final SchemaParser parser;
    private final ValidationService validator;
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    public ThymeleafInvoicePdfRenderer(TenantProfileRepository tenantProfileRepo,
                                       TemplateRepository templates,
                                       @Qualifier("dbTemplateEngine") TemplateEngine engine,
                                       SchemaParser parser,
                                       ValidationService validator) {


        this.engine = engine;
        this.tenantProfileRepo = tenantProfileRepo;
        this.templates = templates;
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public byte[] renderPdf(Invoice invoice) {

        TenantProfile supplier = tenantProfileRepo.findById(invoice.getTenantId())
                .orElseThrow(() -> new IllegalStateException("TenantProfile mancante per tenant " + invoice.getTenantId()));

        String supplierAddress = joinNonBlank(
                supplier.getAddressLine1(),
                supplier.getAddressLine2(),
                joinNonBlank(supplier.getPostalCode(), supplier.getCity(), supplier.getProvince()),
                supplier.getCountry()
        );

        String issuedAtFormatted = invoice.getIssuedAt() == null
                ? ""
                : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY)
                .withZone(ZONE)
                .format(invoice.getIssuedAt());
        String verifyUrl = "https://app.vercert.org.it/v/invoices/" + invoice.getPublicCode();
        Function<Long, String> fmtMoney = (minor) -> formatEuroIT(minor);
        Map<String,Object> userVars = new HashMap<>();
        userVars.put("invoice", invoice);
        userVars.put("supplier", supplier);
        userVars.put("supplierAddress", supplierAddress);
        userVars.put("issuedAtFormatted", issuedAtFormatted);
        userVars.put("verifyUrl", verifyUrl);
        userVars.put("fmtMoney", fmtMoney);
        String html = renderHtml(4L,userVars);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore render PDF fattura", e);
        }
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" • ");
            sb.append(t);
        }
        return sb.toString();
    }
    @Transactional
    public String renderHtml(Long templateId, Map<String,Object> userVars){
        Template tpl = templates.getReferenceById(templateId);
        Context ctx = new Context(Locale.ITALY);
        Map<String,Object> model = new HashMap<>();
        if (userVars != null) model.putAll(userVars);
        ctx.setVariables(model);
        return engine.process(tpl.getHtml(), ctx); // usa StringTemplateResolver
    }
    private static String formatEuroIT(Long minor) {
        if (minor == null) return "";
        // es: 61 -> 0,61 €
        long cents = minor;
        long abs = Math.abs(cents);
        long eur = abs / 100;
        long c = abs % 100;
        String s = eur + "," + (c < 10 ? "0" + c : c) + " €";
        return cents < 0 ? "-" + s : s;
    }
}
