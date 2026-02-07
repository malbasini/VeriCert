package com.example.vericert.service;

import com.example.vericert.domain.*;
import com.example.vericert.dto.SignupRequest;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

//Insert Tenant, User, Membership
@Service
public class SignupService {

    private final TemplateRepository templateRepo;
    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final PasswordEncoder encoder;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanDefinitionRepository planDefinitionRepo;
    private final TenantProfileRepository tenantProfileRepository;
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    public SignupService(TenantRepository tenantRepo,
                         UserRepository userRepo,
                         MembershipRepository membershipRepo,
                         PasswordEncoder encoder,
                         TenantSettingsRepository tenantSettingsRepo,
                         PlanDefinitionRepository planDefinitionRepo,
                         TenantProfileRepository tenantProfileRepository,
                         TemplateRepository templateRepo) {


        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.membershipRepo = membershipRepo;
        this.encoder = encoder;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.planDefinitionRepo = planDefinitionRepo;
        this.tenantProfileRepository = tenantProfileRepository;
        this.templateRepo = templateRepo;
    }

    @Transactional
    public User signup(SignupRequest req) {
        // 1) trova o crea il tenant
        Tenant tenant = null;
        tenant = tenantRepo.findByName(req.tenantName())
                .orElseGet(() -> {
                    Tenant t = new Tenant();
                    t.setName(req.tenantName());
                    t.setStatus(String.valueOf(Status.ACTIVE));
                    return tenantRepo.save(t);
                });

        ensureTenantProfileExists(tenant.getId(), req.tenantName());

        // 2) crea l’utente (se non esiste)
        User user = userRepo.findByUserName(req.username()).orElseGet(() -> {
            User u = new User();
            u.setUserName(req.username());
            u.setPassword(encoder.encode(req.password()));
            u.setEmail(req.email());
            u.setFullName(req.fullname());
            return userRepo.save(u);
        });

        // 3) LOCK sul tenant per evitare race condition sul “primo admin”
        Tenant locked = tenantRepo.lockById(tenant.getId());

        // 4) calcola ruolo: se zero users → ADMIN, altrimenti ruolo base
        long count = membershipRepo.countByTenantId(locked.getId());
        Role roleToAssign = (count == 0) ? Role.ADMIN : Role.VIEWER; // o VIEWER

        // 5) crea users (se non esiste per (tenant,user))
        boolean already = membershipRepo.existsByTenantIdAndUserId(tenant.getId(), user.getId());
        if (!already) {
            Membership m = new Membership();
            m.setTenant(locked);
            m.setUser(user);
            m.setRole(Role.valueOf(roleToAssign.name()));
            m.setStatus(Status.ACTIVE);
            m.setTenantId(tenant.getId());
            m.setUserId(user.getId());
            MembershipId id = new MembershipId(tenant.getId(), user.getId());
            m.setId(id);
            id.setTenantId(tenant.getId());
            id.setUserId(user.getId());
            membershipRepo.save(m);
            TenantSettings t = tenantSettingsRepo.findByTenantId(locked.getId()).orElse(null);
            if (t == null)
                insertTenantSettingsAndTemplate(locked, user);
            return user;
        }
        else {
            TenantSettings t = tenantSettingsRepo.findByTenantId(locked.getId()).orElse(null);
            if (t == null)
                insertTenantSettingsAndTemplate(locked, user);
            return user;
        }

    }
    @Transactional
    public void ensureTenantProfileExists(Long tenantId, String tenantName) {
        if (tenantProfileRepository.existsById(tenantId)) return;
        TenantProfile p = new TenantProfile();
        p.setTenantId(tenantId);
        // default “minimo” (poi l’utente li completa da impostazioni)
        p.setCompanyName(tenantName);
        p.setVatNumber("DA_COMPILARE");
        p.setCountry("IT");
        p.setSupportEmail("support@app.vercert.org"); // o null
        tenantProfileRepository.save(p);
    }
    protected void insertTenantSettingsAndTemplate(Tenant locked, User user) {
        try {
            TenantSettings t = new TenantSettings();
            t.setTenantId(locked.getId());
            t.setEmail(user.getEmail());
            t.setJsonSettings("{\"profile\": {\"website\": \"https://www.acme.it\", \"displayName\": \"ACME Training S.r.l.\", \"contactEmail\": \"info@acme.it\"}, \"branding\": {\"logoUrl\": \"https://app.vercert.org/files/16/logo.png\", \"issuerName\": \"Dott. Mario Rossi\", \"issuerRole\": \"Direttore Formazione\", \"primaryColor\": \"#0d6efd\", \"defaultTemplateId\": 4, \"signatureImageUrl\": \"https://app.vercert.org/files/16/signature.png\"}}");
            t.setStatus(String.valueOf(Status.ACTIVE));
            t.setPlanCode("FREE");
            PlanDefinition pd = planDefinitionRepo.findByCode("FREE").orElseThrow();
            t.setCertsPerMonth(pd.getCertsPerMonth());
            t.setApiCallPerMonth(pd.getApiCallsPerMonth());
            t.setBillingCycle("MONTHLY");
            t.setStorageMb(BigDecimal.valueOf(pd.getStorageMb()));
            t.setSupport("EMAIL");
            // Usa ZonedDateTime.now con la tua ZONE
            ZonedDateTime nowInRome = ZonedDateTime.now(ZONE);
            ZonedDateTime endInRome = switch (t.getBillingCycle()) {
                case "ANNUAL" -> nowInRome.plusYears(1);
                default -> nowInRome.plusMonths(1);
            };
            // Quando converti in Instant, Java sottrae l'offset (es. -1 ora) per portarlo in UTC
            t.setCurrentPeriodStart(nowInRome.toInstant());
            t.setCurrentPeriodEnd(endInRome.toInstant());
            tenantSettingsRepo.save(t);
            //INSERT TEMPLATE
            List<Template> templates = templateRepo.findAllByTenantZero();
            //CONTROLLO CHE IL TEMPLATE NON ESISTA PER IL TENANT CORRENTE
            Template temp = templateRepo.findByTenant(locked).orElse(null);
            if (temp != null) return;
            for (Template t1 : templates) {
                Template template = new Template();
                template.setTenant(locked);
                template.setName(t1.getName());
                template.setVersion(t1.getVersion());
                template.setHtml(t1.getHtml());
                template.setUserVarSchema(t1.getUserVarSchema());
                template.setSysVarsSchema(t1.getSysVarsSchema());
                template.setActive(t1.isActive());
                templateRepo.save(template);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore inserimento tabelle di raccordo", e);
        }
    }

}
