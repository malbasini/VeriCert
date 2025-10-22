package com.example.vericert.controller;

import com.example.vericert.dto.UserRow;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final MembershipRepository membershipRepository;

    public AdminUserController(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
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
}
