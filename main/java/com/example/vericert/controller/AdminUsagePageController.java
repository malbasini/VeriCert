package com.example.vericert.controller;

import com.example.vericert.service.UsageMeterService;
import com.example.vericert.dto.DailyUsageDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/usage")
public class AdminUsagePageController {

    private final UsageMeterService usageMeterService;

    public AdminUsagePageController(UsageMeterService usageMeterService) {
        this.usageMeterService = usageMeterService;
    }

    @GetMapping("/detail/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String usageDetail(
            @PathVariable("tenantId") Long tenantId,
            Model model
    ) {

        // ultimi 7 giorni, incluso oggi
        List<DailyUsageDTO> history7days =
                usageMeterService.getUsageHistoryForTenant(tenantId, 7);

        model.addAttribute("tenantId", tenantId);
        model.addAttribute("history7days", history7days);

        return "usage/usage_detail"; // creeremo questo template
    }
}
