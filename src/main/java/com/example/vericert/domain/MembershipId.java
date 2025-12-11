package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MembershipId implements Serializable {
        @Column(name = "tenant_id")
        private Long tenantId;
        @Column(name = "user_id")
        private Long userId;
        // getter/setter con camelCase coerenti con i field names
        public Long getTenantId() {
            return tenantId;
        }
        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }
        public Long getUserId() {
            return userId;
        }
        public void setUserId(Long userId) {
            this.userId = userId;
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
