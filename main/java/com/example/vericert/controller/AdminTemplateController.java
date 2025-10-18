package com.example.vericert.controller;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/templates")
public class AdminTemplateController {

    private final TemplateRepository repo;

    public AdminTemplateController(TemplateRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String q,
                       @PageableDefault(size=10, sort="updatedAt", direction=Sort.Direction.DESC) Pageable pageable,
                       Model model) {

        Long tenantId = user.getTenantId(); // oppure risolto da TenantResolver
        String tenantName = user.getTenantName();
        Page<Template> page = (q == null || q.isBlank())
                ? repo.findAllByTenantId(tenantId, pageable)
                : repo.searchByName(tenantId, q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("q", q);
        return "admin/templates/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Nuovo template");
        model.addAttribute("active", "templates");
        // … aggiungi oggetto vuoto
        return "admin/templates/create";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Template t = repo.findById(id).orElseThrow();
        model.addAttribute("template", t);
        model.addAttribute("pageTitle", "Modifica template");
        model.addAttribute("active", "templates");
        return "admin/templates/update";
    }
    @GetMapping("/{id}/preview")
    public String preview(@AuthenticationPrincipal CustomUserDetails user,
                          @PathVariable Long id,
                          Model model) {
        Template t = repo.findById(id).orElseThrow();
        String tenantName = user.getTenantName();
        model.addAttribute("template", t);
        model.addAttribute("tenant", tenantName);
        model.addAttribute("pageTitle", "Preview template");
        model.addAttribute("active", "templates");
        return "admin/templates/preview";
    }
}
