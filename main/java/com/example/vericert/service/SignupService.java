package com.example.vericert.service;

import com.example.vericert.domain.*;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
//Insert Tenant, User, Membership
@Service
public class SignupService {

    private final TenantRepository tenantRepo;

    private final UserRepository userRepo;

    private final MembershipRepository membershipRepo;

    private final PasswordEncoder enc;


    public SignupService(TenantRepository tenantRepo,
                         UserRepository userRepo,
                         MembershipRepository membershipRepo,
                         PasswordEncoder enc) {

        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.membershipRepo = membershipRepo;
        this.enc = enc;
    }

    @Transactional
    public void createTenantWithOwner(String slug, String name, String email, String rawPassword){
        Tenant t = new Tenant(); t.setSlug(slug); t.setName(name); t.setPlan(Plan.FREE);
        t = tenantRepo.save(t);
        User u = new User(); u.setEmail(email); u.setPassword(enc.encode(rawPassword)); u = userRepo.save(u);
        MembershipId mid = new MembershipId(t.getId(), u.getId());
        Membership m = new Membership();
        m.setId(mid);
        m.setTenant(t);
        m.setUser(u);
        m.setRole("OWNER");
        membershipRepo.save(m);
    }
}