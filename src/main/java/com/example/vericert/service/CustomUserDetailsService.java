package com.example.vericert.service;

import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
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
        var user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        // membership + tenant già risolti QUI (in transazione)
        var membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UsernameNotFoundException("Nessuna membership trovata"));

        var tenant = membership.getTenant(); // OK se sei in transazione
        var role = membership.getRole().name();     // es. "ADMIN" o "USER"

        var auths = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        return new CustomUserDetails(
                user.getId(),
                tenant.getId(),
                tenant.getName(),
                user.getUserName(),
                user.getPassword(),
                user.getEmail(),
                auths,
                /* enabled */ true
        );
    }
}
