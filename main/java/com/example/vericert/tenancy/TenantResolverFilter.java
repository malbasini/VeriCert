package com.example.vericert.tenancy;

import com.example.vericert.repo.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.lang.annotation.Annotation;

@Component
@Order(1)
public class TenantResolverFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepo;

    public TenantResolverFilter(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String slug = req.getHeader("X-Tenant");
        if (slug != null && !slug.isBlank()) {
            Long id = tenantRepo.findIdBySlug(slug).orElse(null);
            if (id != null) TenantContext.set(id);
        }
        try { chain.doFilter(req, res); }
        finally { TenantContext.clear(); }
    }
}