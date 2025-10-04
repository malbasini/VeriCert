package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.User;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

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
        // Trova user
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        // Trova membership con tenant
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UsernameNotFoundException("Nessuna membership trovata"));

        String role = membership.getRole(); // es: "ADMIN"

        return new CustomUserDetails(
                user.getUserName(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                membership.getTenant().getId(),
                membership.getTenant().getName()
        );
    }
}