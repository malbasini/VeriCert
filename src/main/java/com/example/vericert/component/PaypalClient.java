package com.example.vericert.component;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
public class PaypalClient {

    private final PaymentsProps.PaypalProps props;
    private final WebClient webClient;
    private String cachedAccessToken;
    private Instant accessTokenExpiry;

    public PaypalClient(PaymentsProps paymentsProps,
                        @Qualifier("paypalWebClient") WebClient paypalWebClient) {
        this.props = paymentsProps.getPaypal();
        this.webClient = paypalWebClient;
    }

    /**
     * Recupera (e cache-a) un access token OAuth2 client_credentials.
     */
    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && accessTokenExpiry != null
                && Instant.now().isBefore(accessTokenExpiry.minusSeconds(60))) {
            return cachedAccessToken;
        }

        // grant_type=client_credentials
        Map<String, Object> response = webClient.post()
                .uri("/v1/oauth2/token")
                .headers(h -> h.setBasicAuth(props.getClientId(), props.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String token = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");

        this.cachedAccessToken = token;
        this.accessTokenExpiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3000);

        return token;
    }

    public <T> T get(String path, Class<T> clazz) {
        String token = getAccessToken();

        return webClient.get()
                .uri(path)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(clazz)
                .block();
    }

    public <T> T post(String path, Object body, Class<T> clazz) {
        String token = getAccessToken();

        return webClient.post()
                .uri(path)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), Object.class)
                .retrieve()
                .bodyToMono(clazz)
                .block();
    }
}