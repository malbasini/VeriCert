package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.User;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

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
        // recupero utente
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));

        // recupero membership
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UsernameNotFoundException("Nessuna membership per utente: " + username));

        // attenzione: membership.getRole() contiene "ADMIN" oppure "USER"
        String role = membership.getRole();

        // costruisco l’autorità con prefisso ROLE_
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword()) // deve essere già codificata con BCrypt
                .authorities(Collections.singletonList(authority))
                .build();
    }
}