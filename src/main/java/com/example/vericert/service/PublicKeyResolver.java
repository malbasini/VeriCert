package com.example.vericert.service;

import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.repo.SigningKeyRepository;
import com.example.vericert.util.PemUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// PublicKeyResolver.java
@Service
public class PublicKeyResolver {
    private final SigningKeyRepository repo;
    private final Map<String, PublicKey> cache = new ConcurrentHashMap<>();

    public PublicKeyResolver(SigningKeyRepository repo) { this.repo = repo; }

    @PostConstruct
    public void load() { refresh(); }

    @Scheduled(fixedDelay = 300_000) // ogni 5 min
    public void refresh() {
        try {
            for (SigningKeyEntity k : repo.findAllUsable()) {
                PublicKey pub = PemUtils.readEd25519PublicKeyX509(k.getPublicKeyPem());
                cache.put(k.getKid(), pub);
            }
        } catch (Exception e) {
            // log warning
        }
    }

    public PublicKey resolve(String kid) { return cache.get(kid); }
}
