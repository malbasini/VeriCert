package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.User;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Utente non trovato con username: " + username);
        }
        Membership membership = membershipRepository.findByUser(user);

        // Un solo ruolo
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + membership.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                List.of(authority)
        );
    }
}