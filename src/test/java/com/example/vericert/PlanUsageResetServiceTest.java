package com.example.vericert;

// src/test/java/com/example/vericert/service/PlanUsageResetServiceTest.java
import com.example.vericert.enumerazioni.PlanLimits;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import com.example.vericert.service.PlanUsageResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlanUsageResetServiceTest {

    private UsageMeterRepository usageMeterRepo;
    private TenantSettingsRepository tenantSettingsRepo;
    private PlanUsageResetService service;

    @BeforeEach
    void setUp() {
        usageMeterRepo = mock(UsageMeterRepository.class);
        tenantSettingsRepo = mock(TenantSettingsRepository.class);
        service = new PlanUsageResetService(usageMeterRepo, tenantSettingsRepo);
    }

    @Test
    void resetUsageForNewPeriod_resetsCountersAndAlignsLimits() {
        Long tenantId = 1L;

        TenantSettings settings = new TenantSettings();
        settings.setTenantId(tenantId);
        settings.setPlanCode("BUSINESS"); // mappa su PlanLimits.BUSINESS
        when(tenantSettingsRepo.findById(tenantId)).thenReturn(Optional.of(settings));

        when(usageMeterRepo.resetUsageForTenant(eq(tenantId), any())).thenReturn(3);

        service.resetUsageForNewPeriod(tenantId);

        verify(usageMeterRepo).resetUsageForTenant(eq(tenantId), any());
        verify(tenantSettingsRepo).save(settings);

        PlanLimits limits = PlanLimits.BUSINESS;
        assertThat(settings.getCertsPerMonth()).isEqualTo(limits.getCertsPerMonth());
        assertThat(settings.getApiCallPerMonth()).isEqualTo(limits.getApiCallsPerMonth());
        assertThat(settings.getStorageMb()).isEqualTo(limits.getStorageMbPerMonth());
    }

    @Test
    void resetUsageForNewPeriod_throwsIfSettingsMissing() {
        when(tenantSettingsRepo.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.resetUsageForNewPeriod(99L)
        );
    }
}
