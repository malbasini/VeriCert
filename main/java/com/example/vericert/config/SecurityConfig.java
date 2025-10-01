package com.example.vericert.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${vericert.security.api-key-header}")
    String apiKeyHeader;
    @Value("${vericert.security.api-key}")
    String apiKey;

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http
                .authorizeHttpRequests(auth -> auth
                        // Actuator: health e info pubblici
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v/**", "/files/**","/certificati","/signup","/home","/login","/","/webhooks/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Tutti gli altri actuator richiedono autenticazione
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Il resto della tua app segue le regole normali
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults()) // login form classico
                .httpBasic(Customizer.withDefaults()); // utile per API client
        return http.build();
    }
    public class ApiKeyFilter extends OncePerRequestFilter {
        private final String header;
        private final String expected;

        public ApiKeyFilter(String header, String expected) {
            this.header = header;
            this.expected = expected;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            if (req.getRequestURI().startsWith("/api/")) {
                String key = req.getHeader(header);
                if (!Objects.equals(key, expected)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken("api-key", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(req, res);
        }
    }

    // 👇 User in memoria per test
    @Bean
    public UserDetailsService inMemoryUsers(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}