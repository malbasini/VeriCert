package com.example.vericert.config;


import com.example.vericert.service.CustomUserDetailsService;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final DataSource dataSource;

    public SecurityConfig(CustomUserDetailsService userDetailsService, DataSource dataSource) {
        this.userDetailsService = userDetailsService;
        this.dataSource = dataSource;
    }

    // 🔑 Config della security
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                                .ignoringRequestMatchers(
                                        "/v/**",
                                        "/signup",// signup via fetch JSON
                                        "/admin/**",
                                        "/pricing/billing",       // toggle mensile/annuale sulla index
                                        "/api/**",                // tutte le API (Stripe, PayPal, FREE, ecc.)
                                        "/webhooks/**",
                                        "/api/paypal/webhook",
                                        "/api/stripe/webhook"// Stripe / PayPal webhooks
                                )
                        )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/403", "/error/**","/css/**","/vendor/**", "/js/**", "/images/**").permitAll() //
                         .requestMatchers("/certificati","/revoke").permitAll()
                        .requestMatchers(
                                "/sitemap.xml",
                                "/robots.txt"
                        ).permitAll()
                        // ✅ QUI: storage filesystem (logo/signature)
                        .requestMatchers(HttpMethod.GET, "/storage/**").permitAll()
                        .requestMatchers("/api/paypal/webhook").permitAll()
                        .requestMatchers("/api/stripe/webhook").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasAnyRole("ADMIN","MANAGER","ISSUER","VIEWER")
                        .requestMatchers("/public/verify-file","/v/verify-file-upload").permitAll()
                        .requestMatchers("/templates/**","/certificates/**").authenticated()
                        .requestMatchers("/vui/**","/signup","/login","/index","/files/**","/actuator/health").permitAll()
                        .requestMatchers("/v/**").permitAll()
                        .requestMatchers("/checkout/**").permitAll()
                        .requestMatchers("/privacy", "/firma-digitale","/docs","/faq","/come-funziona","/contact","/").permitAll()
                        .anyRequest().authenticated()
                )
                        .formLogin(form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/index", true)   // ora ti manda lì dopo login
                                .permitAll()
                        )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Configurazione rememberMe
                        .rememberMe(rememberMe -> rememberMe
                                .rememberMeParameter("rememberMe")
                                .tokenValiditySeconds(2 * 24 * 60 * 60)
                                .key("mykey")
                                .userDetailsService(userDetailsService)
                                .tokenRepository(persistentTokenRepository(dataSource))
                        );
                return http.build();
    }
    // 🔑 BCrypt encoder (obbligatorio)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🔑 Provider che usa il tuo CustomUserDetailsService
    @Bean
    public DaoAuthenticationProvider authProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(encoder);
        return authProvider;
    }

    // 🔑 AuthenticationManager da esporre al contesto
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false);
        return repo;
    }
    @Bean
    @ConfigurationPropertiesBinding
    public VericertProps vericertProps() {
        return new VericertProps();
    }


    @Bean
    public HttpFirewall allowPropfindHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // Aggiungiamo PROPFIND alla lista dei metodi consentiti
        firewall.setAllowedHttpMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "PROPFIND"
        ));
        return firewall;
    }

    @Bean
    public PublicKey ed25519PublicKey() throws Exception {
        Path path = Path.of("keys/ed25519-public.pem");
        String pem = Files.readString(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(pem);
        KeyFactory kf = KeyFactory.getInstance("EdDSA");
        return kf.generatePublic(new X509EncodedKeySpec(encoded));
    }

}