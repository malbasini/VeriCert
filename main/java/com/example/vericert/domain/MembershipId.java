package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MembershipId implements Serializable {
    @Column(name="tenant_id")
    private Long tenantId; // mappa tenant_id
    @Column(name="user_id",insertable = false,updatable = false)
    private Long userId;


    public Long getTenant_id() {
        return tenantId;
    }

    public void setTenant_id(Long tenant_id) {
        this.tenantId = tenant_id;
    }

    public Long getUser_id() {
        return userId;
    }

    public void setUser_id(Long user_id) {
        this.userId = user_id;
    }


    public MembershipId() {}

    public MembershipId(Long tenantId, Long userId) {
        this.tenantId = tenantId;
        this.userId = userId;
    }






    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MembershipId that = (MembershipId) o;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, userId);
    }
}
