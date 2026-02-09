package com.example.vericert.config;

import com.example.vericert.component.PaymentsProps;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class PaypalWebClientConfig {

    @Bean(name = "paypalWebClient")
    public WebClient paypalWebClient(PaymentsProps paymentsProps) {
        var props = paymentsProps.getPaypal();

        HttpClient httpClient = HttpClient.create()
                // timeout “classici”
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30))
                        .addHandlerLast(new WriteTimeoutHandler(30))
                )
                // ✅ handshake timeout: lo setti sul Builder restituito da sslContext(...)
                .secure(ssl -> ssl
                        .sslContext(Http11SslContextSpec.forClient())
                        .handshakeTimeout(Duration.ofSeconds(30))
                );

        return WebClient.builder()
                .baseUrl(props.getApiBaseUrl()) // es: https://api.paypal.com
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
