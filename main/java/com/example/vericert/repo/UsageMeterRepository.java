package com.example.vericert.repo;

import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterKey;
import com.example.vericert.dto.DailyUsageDTO;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageMeterRepository extends JpaRepository<UsageMeter, UsageMeterKey> {

    @Query("""
        SELECT u
        FROM UsageMeter u
        WHERE u.id.tenantId = :tenantId
          AND u.id.usageDay = :day
    """)
    Optional<UsageMeter> findByTenantAndDay(
            @Param("tenantId") Long tenantId,
            @Param("day") LocalDate day
    );

    @Query("""
        SELECT new com.example.vericert.dto.DailyUsageDTO(
            u.id.tenantId,
            u.id.usageDay,
            u.certsGenerated,
            u.apiCalls,
            u.pdfStorageMb
        )
        FROM UsageMeter u
        WHERE u.id.tenantId = :tenantId
          AND u.id.usageDay BETWEEN :fromDay AND :toDay
        ORDER BY u.id.usageDay ASC
    """)
    List<DailyUsageDTO> getUsageHistoryForTenant(
            @Param("tenantId") Long tenantId,
            @Param("fromDay") LocalDate fromDay,
            @Param("toDay") LocalDate toDay
    );

    @Query("""
        SELECT new com.example.vericert.dto.DailyUsageDTO(
            u.id.tenantId,
            u.id.usageDay,
            u.certsGenerated,
            u.apiCalls,
            u.pdfStorageMb
        )
        FROM UsageMeter u
        WHERE u.id.usageDay = :day
        ORDER BY u.certsGenerated DESC
    """)
    List<DailyUsageDTO> getTopTenantsToday(
            @Param("day") LocalDate day
    );
}
