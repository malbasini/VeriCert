package com.example.vericert.domain;

import com.example.vericert.enumerazioni.Role;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.tenancy.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

@Entity
@Table(name="membership")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Membership extends BaseTenantEntity {

    @EmbeddedId
    private MembershipId id;
    @Column(name="user_id",nullable = false,insertable = false,updatable = false)
    private Long userId;
    @Column(name="tenant_id",nullable = false,insertable = false,updatable = false)
    private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable=false)
    private Role role;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable=false)
    private Status status = Status.ACTIVE;

    // relazione Tenant → senza duplicare tenant_id
    @MapsId("tenantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    public MembershipId getId() {
        return id;
    }

    public void setId(MembershipId id) {
        this.id = id;
    }

   public Long getUserId() {
        return userId;
    }

    public void setUserId(Long user_id) {
        this.userId = user_id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
