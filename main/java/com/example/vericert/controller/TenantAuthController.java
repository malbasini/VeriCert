package com.example.vericert.controller;

import com.example.vericert.dto.SignupRequest;
import com.example.vericert.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantAuthController {

    private final SignupService signup;

    public TenantAuthController(SignupService signup) {
        this.signup = signup;
    }

    //Insert Tenant, User, Membership
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request){
        signup.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Utente registrato con successo");
    }
}
