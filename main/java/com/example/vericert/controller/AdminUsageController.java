package com.example.vericert.controller;

import com.example.vericert.service.UsageMeterService;
import com.example.vericert.dto.DailyUsageDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/usage")
public class AdminUsageController {

    private final UsageMeterService usageMeterService;

    public AdminUsageController(UsageMeterService usageMeterService) {
        this.usageMeterService = usageMeterService;
    }

    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DailyUsageDTO> getTopTenantsToday() {
        return usageMeterService.getTopTenantsToday();
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DailyUsageDTO> getTenantHistory(
            @PathVariable Long tenantId
    ) {
        // ultimi 7 giorni, incluso oggi
        return usageMeterService.getUsageHistoryForTenant(tenantId, 7);
    }
}
