package com.example.vericert.repo;

import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterKey;
import com.example.vericert.dto.DailyUsageDTO;
import com.example.vericert.dto.UsageTotals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
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
            u.pdfStorageMb,
            u.verificationsCount
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
            u.pdfStorageMb,
            u.verificationsCount
        )
        FROM UsageMeter u
        WHERE u.id.usageDay = :day
        ORDER BY u.certsGenerated DESC
    """)
    List<DailyUsageDTO> getTopTenantsToday(
            @Param("day") LocalDate day
    );

    @Query("""
       SELECT COALESCE(SUM(u.certsGenerated), 0)
       FROM UsageMeter u
       WHERE u.id.tenantId = :tenantId
         AND u.id.usageDay >= :from
         AND u.id.usageDay <= :to
    """)
    Integer sumCertsInPeriod(@Param("tenantId") Long tenantId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    @Query("""
       SELECT COALESCE(SUM(u.apiCalls), 0)
       FROM UsageMeter u
       WHERE u.id.tenantId = :tenantId
        AND u.id.usageDay >= :from
        AND u.id.usageDay <= :to
    """)
    Integer sumApiCallsInPeriod(@Param("tenantId") Long tenantId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("""
       SELECT COALESCE(SUM(u.pdfStorageMb), 0)
       FROM UsageMeter u
       WHERE u.id.tenantId = :tenantId
         AND u.id.usageDay >= :from
         AND u.id.usageDay <= :to
    """)
    BigDecimal sumStorageInPeriod(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);



    /**
     * Azzera i contatori di utilizzo per tutte le righe usage_meter del tenant.
     */
    @Modifying
    @Query("""
           update UsageMeter u
              set u.certsGenerated     = 0,
                  u.apiCalls           = 0,
                  u.pdfStorageMb       = 0,
                  u.verificationsCount = 0,
                  u.lastUpdateTs       = :now
            where u.id.tenantId        = :tenantId
           """)
    int resetUsageForTenant(@Param("tenantId") Long tenantId,
                            @Param("now") Instant now);


    @Query("""
        select new com.example.vericert.dto.UsageTotals(
            coalesce(sum(u.certsGenerated), 0),
            coalesce(sum(u.apiCalls), 0),
            coalesce(sum(u.pdfStorageMb), 0)
        )
        from UsageMeter u
        where u.id.tenantId = :tenantId
          and u.id.usageDay >= :fromDay
          and u.id.usageDay <= :toDay
    """)
    UsageTotals sumUsageForTenantBetweenDays(
            @Param("tenantId") Long tenantId,
            @Param("fromDay") LocalDate fromDay,
            @Param("toDay") LocalDate toDay
    );
}