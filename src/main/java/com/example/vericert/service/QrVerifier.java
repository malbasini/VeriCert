package com.example.vericert.service;

import com.example.vericert.domain.Certificate;
import com.example.vericert.dto.VerificationOutcome;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.util.HashUtils;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import static com.example.vericert.util.Ed25519OkpUtil.extractEd25519XFromSpki;

// QrVerifier.java
@Service
public class QrVerifier {
    private final PublicKeyResolver keyResolver;
    private final VerificationTokenRepository tokenRepo;
    private final Clock clock;
    private final CertificateRepository certificateRepository;

    public QrVerifier(PublicKeyResolver keyResolver,
                      VerificationTokenRepository tokenRepo,
                      CertificateRepository certificateRepository) {
        this.keyResolver = keyResolver;
        this.tokenRepo = tokenRepo;
        this.clock = Clock.systemUTC();
        this.certificateRepository = certificateRepository;
    }

    public VerificationOutcome verify(String compactJws, byte[] certificateBytes) {
        try {
            JWSObject jws = JWSObject.parse(compactJws);
            String kid = jws.getHeader().getKeyID();

            PublicKey pub = keyResolver.resolve(kid);
            if (pub == null) return VerificationOutcome.fail("UNKNOWN_KEY");

            boolean sigOk;
            String keyAlg = pub.getAlgorithm(); // "Ed25519" oppure "RSA" ecc.

            if ("Ed25519".equalsIgnoreCase(keyAlg) || "EdDSA".equalsIgnoreCase(keyAlg)) {
                // ▶︎ Costruisci OKP dalla pubblica X.509
                byte[] x = extractEd25519XFromSpki(pub.getEncoded());
                OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(x))
                        .keyID(kid)
                        .build();
                sigOk = jws.verify(new Ed25519Verifier(okp));
            } else if ("RSA".equalsIgnoreCase(keyAlg)) {
                sigOk = jws.verify(new RSASSAVerifier((RSAPublicKey) pub));
            } else {
                return VerificationOutcome.fail("UNSUPPORTED_KEY_ALG:" + keyAlg);
            }

            if (!sigOk) return VerificationOutcome.fail("INVALID_SIGNATURE");

            var obj = jws.getPayload().toJSONObject();

            Long tenantId = ((Number) obj.get("tenantId")).longValue();
            Long certId   = ((Number) obj.get("certId")).longValue();
            String sha    = (String) obj.get("sha256");
            long   exp    = ((Number) obj.get("exp")).longValue();
            String jti    = (String) obj.get("jti");

            var tokOpt = tokenRepo.findByJti(jti);
            if (tokOpt.isEmpty()) return VerificationOutcome.fail("TOKEN_NOT_FOUND");
            var tok = tokOpt.get();
            Long cert = tok.getCertificateId();
            Certificate c = certificateRepository.findById(cert).orElse(null);
            if (!Objects.equals(tok.getKid(), kid) || !Objects.equals(tok.getCertificateId(), certId)) {
                return VerificationOutcome.fail("TOKEN_MISMATCH");
            }

            Instant now = clock.instant();
            if (now.isAfter(Instant.ofEpochSecond(exp))) return VerificationOutcome.fail("EXPIRED_TOKEN");
            if (tok.getExpiresAt() != null && now.isAfter(tok.getExpiresAt())) return VerificationOutcome.fail("EXPIRED_CERTIFICATE");
            assert c != null;
            if (!(c.getRevokedAt() == null)) return VerificationOutcome.fail("REVOKED");

            String actual = HashUtils.base64UrlSha256(certificateBytes);
            if (!HashUtils.constantTimeEquals(actual, sha)) return VerificationOutcome.fail("CONTENT_MISMATCH");

            return VerificationOutcome.ok(tenantId, certId);

        } catch (JOSEException e) {
            return VerificationOutcome.fail("JOSE_ERROR");
        } catch (Exception e) {
            return VerificationOutcome.fail("ERROR");
        }
    }
}


