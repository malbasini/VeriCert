package com.example.vericert.controller;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.User;
import com.example.vericert.dto.UserRow;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.MembershipAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final MembershipRepository membershipRepository;
    private final MembershipAdminService service;
    private final UserRepository userRepository;
    public AdminUserController(MembershipRepository membershipRepository,
                               MembershipAdminService service,
                               UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listUsers(
                       @RequestParam(name = "q", required = false) String q,
                       @PageableDefault(size = 10, sort = "user.userName") Pageable pageable,
                       Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Long tenantId = user.getTenantId();
        Page<UserRow> page = membershipRepository
                .findUserRowsByTenantAndKeyword(tenantId,
                        (q == null || q.isBlank()) ? null : q.trim().toLowerCase(),
                        pageable);

        model.addAttribute("page", page);
        model.addAttribute("q", q == null ? "" : q);
        return "users/list"; // <- il nome del template HTML qui sotto
    }

    @GetMapping("/{id}/revoke")
    public String revoke(@PathVariable Long id, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Long tenantId = user.getTenantId();
        MembershipId mId = new MembershipId(tenantId, id);
        service.setStatus(mId, Status.SUSPENDED, principal.getName(), principal.getName());
        return "redirect:/admin/users";
    }
    @GetMapping("/{id}/detail")
    public String detail(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Long tenantId = user.getTenantId();
        MembershipId mId = new MembershipId(tenantId, id);
        Membership m = membershipRepository.findById(mId).orElseThrow();
        User usr = userRepository.findById(id).orElseThrow();
        model.addAttribute("membership", m);
        model.addAttribute("user", usr);
        model.addAttribute("active", "users");
        model.addAttribute("pageTitle", "Dettaglio utente");
        model.addAttribute("tenantName", user.getTenantName());
        return "users/detail";
    }
}
