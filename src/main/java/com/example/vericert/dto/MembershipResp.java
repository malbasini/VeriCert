package com.example.vericert.dto;

import com.example.vericert.domain.Membership;

// Role è il tuo enum: ADMIN, MANAGER, ISSUER, VIEWER
public record MembershipResp(
        com.example.vericert.domain.MembershipId id, Long userId, String userName, String email,
        String role, String status
) {
    public static MembershipResp of(Membership m) {
        var u = m.getUser();
        return new MembershipResp(
                m.getId(), u.getId(), u.getUserName(), u.getEmail(),
                m.getRole().name(), m.getStatus().name()
        );
    }
}

