package com.example.vericert.controller;

import com.example.vericert.domain.Membership;
import com.example.vericert.dto.MembershipResp;
import com.example.vericert.dto.PageResp;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("hasRole('ADMIN')")
public class MembershipAdminQueryController {
    private final MembershipRepository repo;

    public MembershipAdminQueryController(MembershipRepository repo) {
        this.repo = repo;
    }
    @GetMapping
    public PageResp<Membership> list(@PageableDefault(size=10, sort="id") Pageable pageable) {
        // opzionale: filtra sul tenant corrente se multi-tenant hard
        Page<Membership> page = repo.findAllByTenantId(currentTenantId(), pageable);
        return PageResp.of(page);
    }
    private Long currentTenantId()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
