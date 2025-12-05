package com.example.vericert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity  // abilita @PreAuthorize
public class MethodSecurityConfig { }
