package com.example.vericert.controller;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.User;
import com.example.vericert.dto.UserRow;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.MembershipAdminService;
import com.example.vericert.service.MembershipSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("hasRole('ADMIN')")
public class MembershipAdminController {

    private final MembershipRepository membershipRepository;
    private final MembershipAdminService service;

    public MembershipAdminController(MembershipAdminService s,
                                     MembershipRepository membershipRepository)
    {
        this.service = s;
        this.membershipRepository = membershipRepository;

    }
    @GetMapping()
    public Page<UserRow> listUser(
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {


        Pageable pageable = PageRequest.of(page, size, Sort.by("user.userName").ascending());

        var spec = Specification.where(MembershipSpecs.byTenant(getCurrentTenantId()))
                .and(MembershipSpecs.keyword(q));

        Page<Membership> p = membershipRepository.findAll(spec, pageable);

        return p.map(m -> new UserRow(
                m.getUser().getId(),
                m.getUser().getUserName(),
                m.getUser().getEmail(),
                m.getRole(),
                m.getStatus()
        ));
    }
    public record ChangeRoleReq(String role) {}


    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleReq req, Principal principal) {
        MembershipId mid = new MembershipId(id, getCurrentTenantId());
        service.changeRole(mid, Role.valueOf(req.role().toUpperCase()), principal.getName());
        return ResponseEntity.noContent().build();
    }

    public record BulkReq(List<Long> ids, String value) {}
    @PatchMapping("/bulk/role")
    public ResponseEntity<?> bulkRole(@RequestBody BulkReq req, Principal p){
        Long tenantId = getCurrentTenantId();
        List<MembershipId> mid = new java.util.ArrayList<>(List.of());
        for (Long id : req.ids) {
            if (id == null) continue;
            mid.add(new MembershipId(tenantId, id));
        }
        service.bulkRole(mid, Role.valueOf(req.value().toUpperCase()), p.getName());
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/bulk/status")
    public ResponseEntity<?> bulkStatus(@RequestBody BulkReq req, Principal p){
        Long tenantId = getCurrentTenantId();
        List<MembershipId> mid = new java.util.ArrayList<>(List.of());
        for (Long id : req.ids) {
            if (id == null) continue;
            mid.add(new MembershipId(tenantId,id));
        }
        service.bulkStatus(mid, Status.valueOf(req.value().toUpperCase()), p.getName(), p.getName());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }


    @GetMapping(value="/export.csv", produces="text/csv")
    public ResponseEntity<String> export(@RequestParam(required=false) String q) {
        var page = service.list(q, PageRequest.of(0, 10_000));
        var sb = new StringBuilder("id,userName,email,role,status\n");
        page.content().forEach(m -> sb.append(m.id().getUserId()).append(',')
                .append(escape(m.userName())).append(',').append(escape(m.email()))
                .append(',').append(m.role()).append(',').append(m.status()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=users.csv")
                .body(sb.toString());
    }
    private static String escape(String s){ return s==null? "": s.replace("\"","\"\""); }
}
