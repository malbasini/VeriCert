package com.example.vericert.service;

import com.example.vericert.dto.VerificationOutcome;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.util.HashUtils;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.OctetKeyPair;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

// QrVerifier.java
@Service
public class QrVerifier {
    private final PublicKeyResolver keyResolver;
    private final VerificationTokenRepository tokenRepo;
    private final Clock clock;

    public QrVerifier(PublicKeyResolver keyResolver,
                      VerificationTokenRepository tokenRepo) {
        this.keyResolver = keyResolver;
        this.tokenRepo = tokenRepo;
        this.clock = Clock.systemUTC();
    }

    public VerificationOutcome verify(String compactJws,
                                      byte[] certificateBytes) {
        try {
            JWSObject jws = JWSObject.parse(compactJws);
            String kid = jws.getHeader().getKeyID();

            PublicKey pub = keyResolver.resolve(kid);
            if (pub == null) return VerificationOutcome.fail("UNKNOWN_KEY");

            boolean sigOk = jws.verify(new com.nimbusds.jose.crypto.Ed25519Verifier((OctetKeyPair) pub));
            if (!sigOk) return VerificationOutcome.fail("INVALID_SIGNATURE");

            var obj = jws.getPayload().toJSONObject();

            Long tenantId = ((Number)obj.get("tenantId")).longValue();
            Long certId   = ((Number)obj.get("certId")).longValue();
            String sha    = (String)obj.get("sha256");
            long exp      = ((Number)obj.get("exp")).longValue();
            String jti    = (String)obj.get("jti");

            // token dal DB (per revoca/exp server-side)
            var tokOpt = tokenRepo.findByJti(jti);
            if (tokOpt.isEmpty()) return VerificationOutcome.fail("TOKEN_NOT_FOUND");
            var tok = tokOpt.get();

            // coerenza base
            if (!Objects.equals(tok.getKid(), kid)
                    || !Objects.equals(tok.getCertificateId(), certId)) {
                return VerificationOutcome.fail("TOKEN_MISMATCH");
            }

            Instant now = clock.instant();

            // scadenza lato token JWS
            if (now.isAfter(Instant.ofEpochSecond(exp))) {
                return VerificationOutcome.fail("EXPIRED_TOKEN");
            }
            // scadenza lato DB
            if (tok.getExpiresAt()!=null && now.isAfter(tok.getExpiresAt())) {
                return VerificationOutcome.fail("EXPIRED_CERTIFICATE");
            }
            // integrità bytes certificato
            String actual = HashUtils.base64UrlSha256(certificateBytes);
            if (!HashUtils.constantTimeEquals(actual, sha)) {
                return VerificationOutcome.fail("CONTENT_MISMATCH");
            }

            return VerificationOutcome.ok(tenantId, certId);

        } catch (Exception e) {
            return VerificationOutcome.fail("ERROR");
        }
    }
}

// VerificationOutcome.java

