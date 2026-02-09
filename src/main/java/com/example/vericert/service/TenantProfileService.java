package com.example.vericert.service;

import com.example.vericert.domain.TenantProfile;
import com.example.vericert.dto.TenantProfileForm;
import com.example.vericert.repo.TenantProfileRepository;
import com.example.vericert.repo.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProfileService {

    private final TenantProfileRepository repo;
    private final TenantRepository tenantRepo; // se vuoi prendere tenant.name come default

    public TenantProfileService(TenantProfileRepository repo, TenantRepository tenantRepo) {
        this.repo = repo;
        this.tenantRepo = tenantRepo;
    }

    @Transactional
    public TenantProfile getOrCreate(Long tenantId) {
        return repo.findById(tenantId).orElseGet(() -> {
            var t = tenantRepo.findById(tenantId).orElseThrow();
            TenantProfile p = new TenantProfile();
            p.setTenantId(tenantId);
            p.setCompanyName(t.getName());
            p.setCountry("IT");
            p.setVatNumber("22");
            return repo.save(p);
        });
    }

    @Transactional
    public TenantProfile update(Long tenantId, TenantProfileForm form) {
        TenantProfile p = getOrCreate(tenantId);

        p.setCompanyName(form.getCompanyName());
        p.setVatNumber(normalizeVat(form.getVatNumber()));
        p.setTaxCode(blankToNull(form.getTaxCode()));
        p.setSdiCode(blankToNull(form.getSdiCode()));
        p.setPecEmail(blankToNull(form.getPecEmail()));

        p.setAddressLine1(blankToNull(form.getAddressLine1()));
        p.setAddressLine2(blankToNull(form.getAddressLine2()));
        p.setCity(blankToNull(form.getCity()));
        p.setProvince(blankToNull(form.getProvince()));
        p.setPostalCode(blankToNull(form.getPostalCode()));
        p.setCountry(form.getCountry()); // fisso Italia per ora

        p.setSupportEmail(blankToNull(form.getSupportEmail()));
        p.setWebsiteUrl(blankToNull(form.getWebsiteUrl()));

        repo.save(p);
        return p;
    }

    public static TenantProfileForm toForm(TenantProfile p) {
        TenantProfileForm f = new TenantProfileForm();
        f.setCompanyName(p.getCompanyName());
        f.setVatNumber(p.getVatNumber());
        f.setTaxCode(p.getTaxCode());
        f.setSdiCode(p.getSdiCode());
        f.setPecEmail(p.getPecEmail());
        f.setAddressLine1(p.getAddressLine1());
        f.setAddressLine2(p.getAddressLine2());
        f.setCity(p.getCity());
        f.setProvince(p.getProvince());
        f.setPostalCode(p.getPostalCode());
        f.setCountry(p.getCountry() != null ? p.getCountry() : "IT");
        f.setSupportEmail(p.getSupportEmail());
        f.setWebsiteUrl(p.getWebsiteUrl());
        return f;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeVat(String vat) {
        if (vat == null) return null;
        return vat.trim().replaceAll("\\s+", "");
    }



}
