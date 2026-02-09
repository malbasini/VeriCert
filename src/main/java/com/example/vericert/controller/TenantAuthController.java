package com.example.vericert.controller;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.User;
import com.example.vericert.dto.SignupRequest;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CaptchaValidator;
import com.example.vericert.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TenantAuthController {

    private final SignupService signup;
    private final TenantRepository tenantRepo;
    private final MembershipRepository membershipRepo;
    private final CaptchaValidator captchaValidator;

    public TenantAuthController(SignupService signup,
                                TenantRepository tenantRepo,
                                MembershipRepository membershipRepo,
                                CaptchaValidator captchaValidator
    ) {
        this.signup = signup;
        this.tenantRepo = tenantRepo;
        this.membershipRepo = membershipRepo;
        this.captchaValidator = captchaValidator;
    }

    //Insert Tenant, User, Membership
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request,
                                    BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        // 1) CAPTCHA
        if (!captchaValidator.verifyCaptcha(request.captchaToken())) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("errors", Map.of("captcha", List.of("Captcha non valido"))));
        }
        String tenantName = request.tenantName();
        Long tenantId = 0L;
        String username = request.username();
        String email = request.email();
        String status="";
        String role="";
        try {
            User user = signup.signup(request);
            tenantName = tenantRepo.findByName(tenantName).get().getName();
            tenantId = tenantRepo.findByName(tenantName).get().getId();
            status = tenantRepo.findByName(tenantName).get().getStatus();
            MembershipId id = new MembershipId(tenantId, user.getId());
            Membership m = membershipRepo.findById(id).orElseThrow();
            role = m.getRole().name();

            return ResponseEntity.ok(new TenantAuthController.VerificationSignup(
                    tenantId,
                    tenantName,
                    status,
                    username,
                    email,
                    role
            ));
        }
        catch (Exception e) {
           return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }
    // DTO interno alla risposta
    record VerificationSignup(
            Long id,
            String name,
            String status,
            String username,
            String email,
            String role
    )
    {}
}