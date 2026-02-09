package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.TenantSettingsDto;
import com.example.vericert.repo.TenantSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class TenantSettingsService {

    private final TenantSettingsRepository repo;
    private final ObjectMapper objectMapper;

    public TenantSettingsService(TenantSettingsRepository repo,
                                 ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }
    public TenantSettingsDto loadForTenant(Long tenantId) {
        TenantSettings ts = repo.findByTenantId(tenantId)
                .orElseGet(() -> new TenantSettings(
                        tenantId,
                        """
                        {
                          "profile": {
                            "displayName": "",
                            "contactEmail": "",
                            "website": ""
                          },
                          "branding": {
                            "issuerName": "",
                            "issuerRole": "",
                            "signatureImageUrl": "",
                            "logoUrl": "",
                            "primaryColor": "#0d6efd",
                            "defaultTemplateId": null
                          }
                        }
                        """.trim()
                ));

        try {
            JsonNode root = objectMapper.readTree(ts.getJsonSettings());

            JsonNode profile = root.path("profile");
            JsonNode branding = root.path("branding");

            return new TenantSettingsDto(
                    profile.path("displayName").asText(""),
                    profile.path("contactEmail").asText(""),
                    profile.path("website").asText(""),

                    branding.path("issuerName").asText(""),
                    branding.path("issuerRole").asText(""),
                    branding.path("logoUrl").asText(""),
                    branding.path("signatureImageUrl").asText(""),
                    branding.path("primaryColor").asText("#0d6efd"),

                    branding.path("defaultTemplateId").isNumber()
                            ? branding.path("defaultTemplateId").asLong()
                            : null
            );
        } catch (Exception e) {
            // fallback grezzo se il JSON è corrotto
            return new TenantSettingsDto(
                    "", "", "",
                    "", "", "", "", "#0d6efd",
                    null
            );
        }
    }

    @Transactional
    public void saveForTenant(Long tenantId, TenantSettingsDto dto) {
        // ricostruiamo il JSON pulito e consistente
        ObjectNodeBuilder b = new ObjectNodeBuilder(objectMapper)
                .putProfile(dto.displayName(), dto.contactEmail(), dto.website())
                .putBranding(
                        dto.issuerName(),
                        dto.issuerRole(),
                        dto.logoUrl(),
                        dto.signatureImageUrl(),
                        dto.primaryColor(),
                        dto.defaultTemplateId()
                );

        String json = b.toJsonString();

        TenantSettings ts = repo.findByTenantId(tenantId)
                .orElse(new TenantSettings(tenantId, json));

        ts.setJsonSettings(json);
        repo.save(ts);
    }
    // piccolo helper interno per costruire il JSON
    private static class ObjectNodeBuilder {
        private final ObjectMapper om;
        private final com.fasterxml.jackson.databind.node.ObjectNode root;
        private final com.fasterxml.jackson.databind.node.ObjectNode profile;
        private final com.fasterxml.jackson.databind.node.ObjectNode branding;

        ObjectNodeBuilder(ObjectMapper om) {
            this.om = om;
            this.root = om.createObjectNode();
            this.profile = om.createObjectNode();
            this.branding = om.createObjectNode();
        }

        ObjectNodeBuilder putProfile(String displayName, String email, String site) {
            profile.put("displayName", displayName);
            profile.put("contactEmail", email);
            profile.put("website", site);
            root.set("profile", profile);
            return this;
        }

        ObjectNodeBuilder putBranding(
                String issuerName,
                String issuerRole,
                String logoUrl,
                String signatureImageUrl,
                String primaryColor,
                Long defaultTemplateId
        ) {
            branding.put("issuerName", issuerName);
            branding.put("issuerRole", issuerRole);
            branding.put("logoUrl", logoUrl);
            branding.put("signatureImageUrl", signatureImageUrl);
            branding.put("primaryColor", primaryColor);
            if (defaultTemplateId != null) {
                branding.put("defaultTemplateId", defaultTemplateId);
            } else {
                branding.putNull("defaultTemplateId");
            }
            root.set("branding", branding);
            return this;
        }

        String toJsonString() {
            try {
                return om.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            } catch (Exception e) {
                throw new RuntimeException("Errore serializzazione settings", e);
            }
        }
    }

    /**
     * Costruisce variabili di sistema comuni usate durante l'emissione dei certificati.
     * Questo NON include serial/code/qr perché quelli sono specifici del certificato
     * e verranno aggiunti dal CertificateService.
     */
    public Map<String,Object> buildBaseSysVarsForTenant(Long tenantId) {
        TenantSettingsDto dto = loadForTenant(tenantId);
        Map<String,Object> map = new HashMap<>();
        map.put("issuerName", dto.issuerName());
        map.put("issuerTitle", dto.issuerRole());
        map.put("themeColor", dto.primaryColor());
        map.put("publicBaseUrl", dto.website());
        map.put("logoUrl", dto.logoUrl());
        map.put("signatureImageUrl", dto.signatureImageUrl());
        // puoi aggiungere anche logoUrl, footerText ecc. in futuro
        return map;
    }
    public Optional<TenantSettings> findPendingBySubscriptionId(long tenantId) {
        return repo.findByTenantId(tenantId);
    }
}
