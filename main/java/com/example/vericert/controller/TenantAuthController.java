package com.example.vericert.controller;

import com.example.vericert.service.SignupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TenantAuthController {

    private final SignupService signup;

    public TenantAuthController(SignupService signup) {
        this.signup = signup;
    }

    //Insert Tenant, User, Membership
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupReq r){
        signup.createTenantWithOwner(r.slug(), r.name(), r.ownerEmail(), r.password());
        return ResponseEntity.ok(Map.of("ok", true));
    }
    public record SignupReq(String slug, String name, String ownerEmail, String password){}
}
