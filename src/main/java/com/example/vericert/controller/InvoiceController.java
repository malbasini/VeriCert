package com.example.vericert.controller;

import com.example.vericert.domain.Invoice;
import com.example.vericert.enumerazioni.InvoiceStatus;
import com.example.vericert.repo.InvoiceRepository;
import com.example.vericert.util.AuthUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceRepository repo;
    public InvoiceController(InvoiceRepository repo) {
        this.repo = repo;
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String list(
            Model model
    ) {
        Long tenantId = AuthUtil.currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "invoices/list";
    }

    @GetMapping("/new")
    public String newPage(Model model) {
        long tenantId = AuthUtil.currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "invoices/new";
    }

    @GetMapping("/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        model.addAttribute("invoiceId", id);
        Invoice invoice = repo.findById(id).get();
        long tenantId = AuthUtil.currentTenantId();
        String tenantName = AuthUtil.me().getTenantName();
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("invoice", invoice);
        model.addAttribute("currentTenant", tenantName);
        return "invoices/detail";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        model.addAttribute("invoiceId", id);
        long tenantId = AuthUtil.currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "invoices/edit";
    }
}
