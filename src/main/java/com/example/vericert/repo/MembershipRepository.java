package com.example.vericert.repo;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.User;
import com.example.vericert.dto.UserRow;
import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId>, JpaSpecificationExecutor<Membership> {
    Optional<Membership> findByUser(User user);
    Membership findByTenant_Id(Long tenantId);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndRoleAndStatus(Long tenantId, Role role, Status status);
    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);
    long countByTenantIdAndRole(Long tenantId, Role role);
    Optional<Membership> findByTenantIdAndUserId(Long tenantId, Long userId);
    Optional<Membership> findById(MembershipId id);
    @Query("select m from Membership m where m.tenant.id = :tenantId")
    Page<Membership> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
    @Query("""
    select m from Membership m
    where m.tenant.id = :tenantId
      and (lower(m.user.userName) like lower(concat('%',:q,'%'))
        or lower(m.user.email) like lower(concat('%',:q,'%')))
  """)
    Page<Membership> search(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);
    @Query("select count(m) from Membership m where m.tenant.id=:tenantId and m.role='ADMIN' and m.status='ACTIVE'")
    long countActiveAdmins(@Param("tenantId") Long tenantId);
    Optional<Membership> findByIdAndTenantId(MembershipId id, Long tenantId);

    @Query("""
       select m from Membership m
       join fetch m.user u
       where u.userName = :username
       order by m.id asc
    """)
    List<Membership> findAllByUsernameWithUser(@Param("username") String username);

    @Query(
            value = """
        select new com.example.vericert.dto.UserRow(
          u.id, u.userName, u.email, m.role, m.status
        )
        from Membership m
          join m.user u
        where m.tenant.id = :tenantId
          and (
            :kw is null
            or lower(u.userName) like concat('%', :kw, '%')
            or lower(u.email)    like concat('%', :kw, '%')
          )
        """,
            countQuery = """
        select count(m)
        from Membership m
          join m.user u
        where m.tenant.id = :tenantId
          and (
            :kw is null
            or lower(u.userName) like concat('%', :kw, '%')
            or lower(u.email)    like concat('%', :kw, '%')
          )
        """
    )
    Page<UserRow> findUserRowsByTenantAndKeyword(
            @Param("tenantId") Long tenantId,
            @Param("kw") String keywordOrNull,
            Pageable pageable
    );
    @Query("""
    select count(m)
    from Membership m
    where m.tenant.id = :tenantId
      and m.status = :status
""")
    long countActiveUsersByTenant(@Param("tenantId") Long tenantId,
                                  @Param("status") com.example.vericert.enumerazioni.Status status);




}

