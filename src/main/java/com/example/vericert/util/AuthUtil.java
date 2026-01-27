package com.example.vericert.util;

import com.example.vericert.service.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {
    private AuthUtil(){}

    public static CustomUserDetails me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            throw new IllegalStateException("Utente non autenticato");
        }
        return cud;
    }

    public static Long currentUserId() { return me().getUserId(); }
    public static Long currentTenantId(){ return me().getTenantId(); }
}
