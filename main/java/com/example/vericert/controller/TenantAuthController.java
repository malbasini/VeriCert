package com.example.vericert.controller;

import com.example.vericert.domain.Membership;
import com.example.vericert.dto.SignupRequest;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TenantAuthController {

    private final SignupService signup;
    private final TenantRepository tenantRepo;
    private final MembershipRepository membershipRepo;

    public TenantAuthController(SignupService signup,
                                TenantRepository tenantRepo,
                                MembershipRepository membershipRepo
    ) {
        this.signup = signup;
        this.tenantRepo = tenantRepo;
        this.membershipRepo = membershipRepo;
    }

    //Insert Tenant, User, Membership
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request,
                                    BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            fe -> fe.getField(),
                            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        String tenantName = request.tenantName();
        Long tenantId = 0L;
        String username = request.username();
        String email = request.email();
        String status="";
        String role="";
        try {
            signup.signup(request);
            tenantName = request.tenantName();
            tenantId = tenantRepo.findByName(tenantName).get().getId();
            status = tenantRepo.findByName(tenantName).get().getStatus();
            Membership m = membershipRepo.findByTenant_Id(tenantId);
            role = m.getRole();
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(new TenantAuthController.VerificationSignup(
                tenantId,
                tenantName,
                status,
                username,
                email,
                role
        ));
    }
    // DTO interno alla risposta
    record VerificationSignup(
            Long id,
            String name,
            String status,
            String username,
            String email,
            String role
    ) {
    }
}