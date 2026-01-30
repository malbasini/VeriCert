package com.example.vericert.controller;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
@RequestMapping("/admin/templates")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminTemplateController {

    private final TemplateRepository repo;

    public AdminTemplateController(TemplateRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
    public String list(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String q,
                       @PageableDefault(size=10, sort="updatedAt", direction=Sort.Direction.DESC) Pageable pageable,
                       Model model) {


        if(Objects.equals(q, "null"))
           q = null;
        Long tenantId = user.getTenantId(); // oppure risolto da TenantResolver
        Page<Template> pageTemplate = (q == null || q.isBlank())
                ? repo.findAllByTenantId(tenantId, pageable)
                : repo.searchByName(tenantId, q, pageable);
        model.addAttribute("page", pageTemplate);
        model.addAttribute("q", q);
        return "template/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Nuovo template");
        model.addAttribute("active", "templates");
        // … aggiungi oggetto vuoto
        return "template/create";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String edit(@PathVariable Long id, Model model) {
        Template t = repo.findById(id).orElseThrow();
        model.addAttribute("template", t);
        model.addAttribute("pageTitle", "Modifica template");
        model.addAttribute("active", "templates");
        return "template/update";
    }
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String preview(@AuthenticationPrincipal CustomUserDetails user,
                          @PathVariable Long id,
                          Model model) {
            Template t = repo.findByTenantIdAndActiveTrue(user.getTenantId()).orElseThrow();
            String tenantName = user.getTenantName();
            model.addAttribute("template", t);
            model.addAttribute("tenant", tenantName);
            model.addAttribute("pageTitle", "Preview template");
            model.addAttribute("active", "templates");
            if("TEMPLATE FATTURE".equalsIgnoreCase(t.getName()))
                return "template/preview_invoice";
            else
                return "template/preview";
    }
}
