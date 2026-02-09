package com.example.vericert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.recaptcha.key")

public class CaptchaSettings {

    private String site;
    private String secret;

    public String getSite() {
        return site;
    }

    public String getSecret() {
        return secret;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

}