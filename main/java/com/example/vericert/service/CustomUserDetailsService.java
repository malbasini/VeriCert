package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.User;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import com.example.vericert.tenancy.TenantContext; // importa la tua classe ThreadLocal
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Recupero utente
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        // Recupero membership con tenant
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UsernameNotFoundException("Nessuna membership trovata"));

        // Imposto tenant corrente nel ThreadLocal
        if (membership.getTenant() != null) {
            Long tenantId = membership.getTenant().getId();
            TenantContext.set(tenantId); // se il tuo TenantContext accetta String
            System.out.println(">>> Tenant corrente impostato: " + tenantId);
        }

        // Ruolo
        String role = membership.getRole(); // es. "ADMIN" o "USER"

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password(user.getPassword()) // deve essere BCrypt
                .roles(role) // Spring aggiunge automaticamente "ROLE_"
                .build();
    }
}