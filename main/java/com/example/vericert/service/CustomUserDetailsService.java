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

import javax.management.relation.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Optional<User> u = Optional.ofNullable(userRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException("Utente non trovato")));
        User user = u.get();
        Optional<Membership> m = Optional.ofNullable(membershipRepository.findByUser(user).orElseThrow(() -> new UsernameNotFoundException("Nessuna membership trovata")));
        Membership membership = m.get();
        // Un solo ruolo
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + membership.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                List.of(authority)
        );
    }
}