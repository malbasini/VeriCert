package com.example.vericert.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vericert.payments")
public class PaymentsProps {

    private String currency;
    private String successUrl;
    private String cancelUrl;
    private StripeProps stripe = new StripeProps();
    private PaypalProps paypal = new PaypalProps();

    // getters/setters
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public StripeProps getStripe() { return stripe; }
    public void setStripe(StripeProps stripe) { this.stripe = stripe; }
    public PaypalProps getPaypal() { return paypal; }
    public void setPaypal(PaypalProps paypal) { this.paypal = paypal; }

    public static class StripeProps {
        private String secretKey;
        private String publishableKey;
        private String webhookSecret;
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }

    public static class PaypalProps {
        private String clientId;
        private String clientSecret;
        private String mode;        // sandbox | live
        private String successUrl;
        private String cancelUrl;
        private String apiBaseUrl;
        private String webhookId;
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getSuccessUrl() { return successUrl; }
        public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
        public String getWebhookId() { return webhookId; }
        public void setWebhookId(String webhookId) { this.webhookId = webhookId; }
    }

}
