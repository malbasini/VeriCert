package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vericert")
public class VericertProps {

    @Value("vericert.public-base-url")
    private String publicBaseUrl;
    @Value("${vericert.signing-kid}")
    private String kid;
    @Value("${vericert.storage.local-path}")
    private String baseUrl;
    @Value("${vericert.storage.local-path}")
    private String storageLocalPath;
    @Value("${vericert.public-base-url-verify}")
    private String baseUrlVerify;

    public String getPublicBaseUrl() { return publicBaseUrl; }

    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public String getKid() {return kid;}

    public void setKid(String kid) {this.kid = kid;}

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getStorageLocalPath() {return storageLocalPath;}

    public void setStorageLocalPath(String storageLocalPath) {this.storageLocalPath = storageLocalPath;}

    public String getPublicBaseUrlVerify() {
        return baseUrlVerify;
    }

    public void setPublicBaseUrlVerify(String baseUrlVerify) {
        this.baseUrlVerify = baseUrlVerify;
    }

}