package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.dto.MembershipResp;
import com.example.vericert.dto.PageResp;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MembershipAdminService {
    private final MembershipRepository repo;
    private final UserRepository userRepo;

    public MembershipAdminService(MembershipRepository repo,UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }
    @Transactional
    public void changeRole(MembershipId membershipId, Role newRole, String actor) {
        var m = repo.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership non trovata"));

        Role old = Role.valueOf(m.getRole().name());
        if (old == newRole) return;

        Long tenantId = m.getTenant().getId();

        // Non togliere l’ultimo ADMIN
        if (old == Role.ADMIN) {
            long admins = repo.countByTenantIdAndRole(tenantId, Role.ADMIN);
            if (admins <= 1) {
                throw new IllegalStateException("Impossibile rimuovere l’ultimo ADMIN del tenant");
            }
        }

        m.setRole(Role.valueOf(newRole.name()));
        repo.save(m);
    }

    @Transactional
    public void setStatus(MembershipId membershipId, Status status, String actor, String currentUsername) {
        Long tid = currentTenantId();
        var m = repo.findByIdAndTenantId(membershipId, tid)
                .orElseThrow(() -> new IllegalArgumentException("Membership non trovata"));

        // opzionale: impedisci auto-sospensione
        if (m.getUser().getUserName().equals(currentUsername) && status==Status.SUSPENDED)
            throw new IllegalStateException("Non puoi sospendere il tuo stesso account");

        m.setStatus(status);
    }

    @Transactional
    public PageResp<MembershipResp> list(String q, Pageable pageable) {
        Long tid = currentTenantId();
        Page<Membership> p = (q==null || q.isBlank()) ? repo.findAllByTenantId(tid, pageable) : repo.search(tid, q.trim(), pageable);
        return PageResp.of(p.map(MembershipResp::of));
    }

    @Transactional
    public void bulkRole(List<MembershipId> ids, Role newRole, String actor) {
        for (MembershipId id : ids) changeRole(id, newRole, actor);
    }

    @Transactional
    public void bulkStatus(List<MembershipId> ids, Status status, String actor, String currentUser) {
        for (MembershipId id : ids) setStatus(id, status, actor, currentUser);
    }

    private Long currentTenantId()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    @Transactional
    public void deleteUser(Long id) throws Exception {
        MembershipId id2 = new MembershipId(currentTenantId(), id);
        try
        {
        repo.deleteById(id2);
        userRepo.deleteById(id);
        }
        catch (Exception e)
        {
            throw new Exception("Errore durante la cancellazione dell'utente");
        }
    }
}
