package com.example.vericert.service;

import com.example.vericert.domain.*;
import com.example.vericert.dto.SignupRequest;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
//Insert Tenant, User, Membership
@Service
public class SignupService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         MembershipRepository membershipRepository,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(SignupRequest req) {
        // 1. crea utente
        var user = new User();
        user.setUserName(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        // 2. crea tenant se non esiste
        Tenant tenant = tenantRepository.findIdByName(req.tenantName())
                .orElseGet(() -> {
                    Tenant t = new Tenant();
                    t.setName(req.tenantName());
                    return tenantRepository.save(t);
                });

        // 3. controlla se ci sono già membership nel tenant
        boolean tenantHasUsers = membershipRepository.existsByTenant(tenant);

        // 4. assegna ruolo
        String role = tenantHasUsers ? "USER" : "ADMIN";

        var membership = new Membership();
        var membershipId = new MembershipId(user.getId(), tenant.getId());
        membership.setId(membershipId);
        membership.setUser(user);
        membership.setTenant(tenant);
        membership.setRole(role);
        membershipRepository.save(membership);
    }
}