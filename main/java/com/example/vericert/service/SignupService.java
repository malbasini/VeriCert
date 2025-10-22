package com.example.vericert.service;

import com.example.vericert.domain.*;
import com.example.vericert.dto.SignupRequest;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

//Insert Tenant, User, Membership
@Service
public class SignupService {
    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final PasswordEncoder encoder;

    public SignupService(TenantRepository tenantRepo,
                         UserRepository userRepo,
                         MembershipRepository membershipRepo,
                         PasswordEncoder encoder) {
        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.membershipRepo = membershipRepo;
        this.encoder = encoder;
    }

    @Transactional
    public void signup(SignupRequest req) {
        // 1) trova o crea il tenant
        Tenant tenant = tenantRepo.findByName(req.tenantName())
                .orElseGet(() -> {
                    Tenant t = new Tenant();
                    t.setName(req.tenantName());
                    t.setStatus(String.valueOf(Status.ACTIVE));
                    return tenantRepo.save(t);
                });

        // 2) crea l’utente (se non esiste)
        User user = userRepo.findByUserName(req.username()).orElseGet(() -> {
            User u = new User();
            u.setUserName(req.username());
            u.setPassword(encoder.encode(req.password()));
            u.setEmail(req.email());
            return userRepo.save(u);
        });

        // 3) LOCK sul tenant per evitare race condition sul “primo admin”
        Tenant locked = tenantRepo.lockById(tenant.getId());

        // 4) calcola ruolo: se zero membership → ADMIN, altrimenti ruolo base
        long count = membershipRepo.countByTenantId(locked.getId());
        Role roleToAssign = (count == 0) ? Role.ADMIN : Role.VIEWER; // o VIEWER

        // 5) crea membership (se non esiste per (tenant,user))
        boolean already = membershipRepo.existsByTenantIdAndUserId(tenant.getId(), user.getId());
        if (!already) {
            Membership m = new Membership();
            m.setTenant(locked);
            m.setUser(user);
            m.setRole(String.valueOf(roleToAssign));
            m.setStatus(Status.ACTIVE);
            membershipRepo.save(m);
        }
        // (opzionale) audit log
    }
}
