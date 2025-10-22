package com.example.vericert.controller;

import com.example.vericert.dto.MembershipResp;
import com.example.vericert.dto.PageResp;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.service.MembershipAdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("hasRole('ADMIN')")
public class MembershipAdminController {
    private final MembershipAdminService service;

    public MembershipAdminController(MembershipAdminService s) { this.service = s; }

    @GetMapping
    public PageResp<MembershipResp> list(@RequestParam(required=false) String q,
                                         @PageableDefault(size=10, sort="id") Pageable pageable,
                                         Principal principal) {
        return service.list(q, pageable);
    }

    public record ChangeRoleReq(String role) {}
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleReq req, Principal principal) {
        service.changeRole(id, Role.valueOf(req.role().toUpperCase()), principal.getName());
        return ResponseEntity.noContent().build();
    }

    public record ChangeStatusReq(String status) {}
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody ChangeStatusReq req, Principal principal) {
        service.setStatus(id, Status.valueOf(req.status().toUpperCase()), principal.getName(), principal.getName());
        return ResponseEntity.noContent().build();
    }

    public record BulkReq(List<Long> ids, String value) {}
    @PatchMapping("/bulk/role")
    public ResponseEntity<?> bulkRole(@RequestBody BulkReq req, Principal p){
        service.bulkRole(req.ids(), Role.valueOf(req.value().toUpperCase()), p.getName());
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/bulk/status")
    public ResponseEntity<?> bulkStatus(@RequestBody BulkReq req, Principal p){
        service.bulkStatus(req.ids(), Status.valueOf(req.value().toUpperCase()), p.getName(), p.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value="/export.csv", produces="text/csv")
    public ResponseEntity<String> export(@RequestParam(required=false) String q) {
        var page = service.list(q, PageRequest.of(0, 10_000));
        var sb = new StringBuilder("id,userName,email,role,status\n");
        page.content().forEach(m -> sb.append(m.id()).append(',')
                .append(escape(m.userName())).append(',').append(escape(m.email()))
                .append(',').append(m.role()).append(',').append(m.status()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=memberships.csv")
                .body(sb.toString());
    }
    private static String escape(String s){ return s==null? "": s.replace("\"","\"\""); }
}
