package com.example.vericert.controller;

import com.example.vericert.dto.DailyUsageDTO;
import com.example.vericert.service.UsageMeterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/usage")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminUsageController {

    private final UsageMeterService usageMeterService;

    public AdminUsageController(UsageMeterService usageMeterService) {

        this.usageMeterService = usageMeterService;
    }

    @GetMapping("/top")
    public List<DailyUsageDTO> getTopTenantsToday() {
        return usageMeterService.getTopTenantsToday();
    }

    @GetMapping("/{tenantId}")
    public List<DailyUsageDTO> getTenantHistory(@PathVariable Long tenantId) {
        // ultimi 7 giorni, incluso oggi
        return usageMeterService.getUsageHistoryForTenant(tenantId, 7);
    }
}
