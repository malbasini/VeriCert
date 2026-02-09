package com.example.vericert.controller;

import com.example.vericert.dto.DailyUsageDTO;
import com.example.vericert.service.UsageMeterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/usage")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminUsagePageController {

    private final UsageMeterService usageMeterService;

    public AdminUsagePageController(UsageMeterService usageMeterService) {

        this.usageMeterService = usageMeterService;
    }

    @GetMapping("/detail/{tenantId}")
    public String usageDetail(@PathVariable Long tenantId, Model model)
    {
        // ultimi 7 giorni, incluso oggi
        List<DailyUsageDTO> history7days = usageMeterService.getUsageHistoryForTenant(tenantId, 7);
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("history7days", history7days);
        return "usage/usage_detail"; // creeremo questo template
    }
}
