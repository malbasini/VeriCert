package com.example.vericert.service;

import com.example.vericert.util.Ed25519OkpUtil;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.nio.file.Path;
import java.util.Map;

public class QrSignerOkp {

    private final OctetKeyPair okp;
    private final String kid;

    public QrSignerOkp(Path privatePem, Path publicPem, String kid) throws Exception {
        this.kid = kid;

        // 1) leggi DER da PEM
        byte[] pkcs8Der = Ed25519OkpUtil.readPemDer(privatePem, "PRIVATE KEY"); // PKCS#8
        byte[] spkiDer  = Ed25519OkpUtil.readPemDer(publicPem,  "PUBLIC KEY");  // X.509 SPKI

        // 2) estrae d (32) e x (32)
        byte[] d = Ed25519OkpUtil.ed25519PrivateDFromPKCS8(pkcs8Der);
        byte[] x = Ed25519OkpUtil.extractEd25519PublicXFromSpki(spkiDer);

        // 3) costruisce l'OKP (JWK)
        this.okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(x))
                .d(Base64URL.encode(d))
                .keyID(kid)
                .build();
    }

    public String sign(Map<String,Object> claims) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(kid)
                .type(JOSEObjectType.JOSE)
                .build();

        String json = JSONObjectUtils.toJSONString(claims);
        JWSObject jws = new JWSObject(header, new Payload(json));

        // ✅ versione OKP del signer
        jws.sign(new Ed25519Signer(okp));
        return jws.serialize();
    }
}
