package com.example.vericert.component;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import org.springframework.stereotype.Component;

@Component
public class PaypalClientFactory {
    private final PayPalHttpClient client;

    public PaypalClientFactory(PaymentsProps props) {
        var pp = props.getPaypal();
        PayPalEnvironment env = "live".equalsIgnoreCase(pp.getMode())
                ? new PayPalEnvironment.Live(pp.getClientId(), pp.getClientSecret())
                : new PayPalEnvironment.Sandbox(pp.getClientId(), pp.getClientSecret());
        this.client = new PayPalHttpClient(env);
    }
    public PayPalHttpClient client() { return client; }
}
