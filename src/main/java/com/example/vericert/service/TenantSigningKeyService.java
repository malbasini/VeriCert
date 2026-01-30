package com.example.vericert.service;

import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.TenantSigningKeyEntity;
import com.example.vericert.repo.SigningKeyRepository;
import com.example.vericert.repo.TenantSigningKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class TenantSigningKeyService {

    private final TenantSigningKeyRepository tenantRepo;
    private final SigningKeyRepository signingRepo;
    private final AesGcmCrypto crypto;

    public TenantSigningKeyService(TenantSigningKeyRepository tenantRepo,
                                   SigningKeyRepository signingRepo,
                                   AesGcmCrypto crypto) {
        this.tenantRepo = tenantRepo;
        this.signingRepo = signingRepo;
        this.crypto = crypto;
    }

    @Transactional
    public SigningKeyEntity ensureTenantKey(Long tenantId, String tenantSlug) throws Exception {
        Optional<SigningKeyEntity> existing = tenantRepo.findActiveSigningKeyByTenant(tenantId);
        if (existing.isPresent()) return existing.get();

        String kid = "tenant-" + tenantId + "-" + LocalDate.now().toString().replace("-", "");
        String p12Password = Passwords.secureRandomBase64(24); // helper sotto

        var mat = TenantKeyGenerator.generate(tenantSlug, p12Password.toCharArray(), 1095);

        SigningKeyEntity sk = new SigningKeyEntity();
        sk.setKid(kid);
        sk.setStatus("ACTIVE");
        sk.setPublicKeyPem(mat.publicKeyPem());
        sk.setCertPem(mat.certPem());
        sk.setP12Blob(mat.p12());
        sk.setP12PasswordEnc(crypto.encryptToBase64(p12Password));
        sk.setNotBeforeTs(LocalDateTime.ofInstant(mat.notBefore(), ZoneId.systemDefault()));
        sk.setNotAfterTs(LocalDateTime.ofInstant(mat.notAfter(), ZoneId.systemDefault()));
        signingRepo.save(sk);

        TenantSigningKeyEntity tsk = new TenantSigningKeyEntity();
        tsk.setTenantId(tenantId);
        tsk.setKid(kid);
        tsk.setStatus("ACTIVE");
        tsk.setAssignedTs(LocalDateTime.now());
        tenantRepo.save(tsk);

        return sk;
    }
        @Transactional
        public SigningKeyEntity rotateTenantKey(Long tenantId, String tenantSlug) throws Exception {
            // 1) Trova chiave attiva tenant (se esiste) e mettila RETIRED
            tenantRepo.findByTenantIdAndStatus(tenantId, "ACTIVE").ifPresent(tsk -> {
                tsk.setStatus("RETIRED");
                tenantRepo.save(tsk);

                signingRepo.findById(tsk.getKid()).ifPresent(sk -> {
                    sk.setStatus("RETIRED");
                    signingRepo.save(sk);
                });
            });

            // 2) Crea nuova chiave
            String kid = "tenant-" + tenantId + "-" + LocalDate.now().toString().replace("-", "") + "-r1";
            String p12Password = Passwords.secureRandomBase64(24);

            var mat = TenantKeyGenerator.generate(tenantSlug, p12Password.toCharArray(), 1095);

            SigningKeyEntity sk = new SigningKeyEntity();
            sk.setKid(kid);
            sk.setStatus("ACTIVE");
            sk.setPublicKeyPem(mat.publicKeyPem());
            sk.setCertPem(mat.certPem());
            sk.setP12Blob(mat.p12());
            sk.setP12PasswordEnc(crypto.encryptToBase64(p12Password));
            sk.setNotBeforeTs(LocalDateTime.ofInstant(mat.notBefore(), ZoneId.systemDefault()));
            sk.setNotAfterTs(LocalDateTime.ofInstant(mat.notAfter(), ZoneId.systemDefault()));
            signingRepo.save(sk);

            TenantSigningKeyEntity tsk = new TenantSigningKeyEntity();
            tsk.setTenantId(tenantId);
            tsk.setKid(kid);
            tsk.setStatus("ACTIVE");
            tsk.setAssignedTs(LocalDateTime.now());
            tenantRepo.save(tsk);

            return sk;
        }
    }

