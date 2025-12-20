package com.example.vericert.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// SigningProperties.java
@Component
@ConfigurationProperties(prefix="vericert.signing")
public class SigningProperties {
    private String alg; // "EdDSA"
    private String kid;
    private String privateKeyPemPath;
    private String publicKeyPemPath;


    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getPrivateKeyPemPath() {
        return privateKeyPemPath;
    }

    public void setPrivateKeyPemPath(String privateKeyPemPath) {
        this.privateKeyPemPath = privateKeyPemPath;
    }

    public String getPublicKeyPemPath() {
        return publicKeyPemPath;
    }

    public void setPublicKeyPemPath(String publicKeyPemPath) {
        this.publicKeyPemPath = publicKeyPemPath;
    }
}



