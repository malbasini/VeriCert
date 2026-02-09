package com.example.vericert.config;

import com.example.vericert.component.SigningProperties;
import com.example.vericert.service.AesGcmCrypto;
import com.example.vericert.service.QrSignerOkp;
import com.example.vericert.util.PemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

// CryptoConfig.java
@Configuration
@EnableScheduling
public class CryptoConfig {
    @Bean
    public PrivateKey signingPrivateKey(SigningProperties props) throws Exception {
        if (!"EdDSA".equalsIgnoreCase(props.getAlg())) {
            throw new IllegalStateException("Algoritmo supportato qui: EdDSA/Ed25519");
        }
        loadEd25519PrivateKey(Path.of(props.getPrivateKeyPemPath()));
        return PemUtils.readEd25519PrivateKeyPKCS8(Path.of(props.getPrivateKeyPemPath()));
    }
    private static void loadEd25519PrivateKey(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath);
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----","")
                .replace("-----END PRIVATE KEY-----","")
                .replaceAll("\\s+","");
        byte[] der = Base64.getDecoder().decode(base64);

        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        kf.generatePrivate(new PKCS8EncodedKeySpec(der));
    }
    @Bean
    public QrSignerOkp qrSignerOkp(SigningProperties props) throws Exception {
        return new QrSignerOkp(
                Path.of(props.getPrivateKeyPemPath()),
                Path.of(props.getPublicKeyPemPath()),
                props.getKid()
        ); // costruttore “vecchio” che avevi
    }

    @Bean
    public AesGcmCrypto aesGcmCrypto(@Value("${vericert.master-key-b64}") String masterKeyB64) {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyB64);

        if (keyBytes.length != 32) {
            throw new IllegalStateException("VERICERT_MASTER_KEY_B64 must decode to exactly 32 bytes (AES-256). Got: " + keyBytes.length);
        }

        return new AesGcmCrypto(keyBytes);
    }
}