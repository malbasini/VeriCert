package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.component.PaypalClientFactory;
import com.example.vericert.domain.Payment;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import com.paypal.orders.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/paypal")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class PayPalController {

    private final PaypalClientFactory factory;
    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final AdminPlanDefinitionsService service;

    public PayPalController(PaypalClientFactory factory,
                            PaymentsProps props,
                            PaymentRepository payRepo,
                            AdminPlanDefinitionsService service) {
        this.factory = factory;
        this.props = props;
        this.payRepo = payRepo;
        this.service = service;
    }

    @PostMapping("/order")
    public Map<String, Object> createOrder(@RequestParam Long tenantId,
                                           @RequestParam Long amountMinor,      // centesimi
                                           @RequestParam(required = false) Long certificateId,
                                           @RequestParam(defaultValue = "EUR") String currency,
                                           @RequestParam(defaultValue = "Certificato VeriCert") String description,
                                           @RequestParam String planCode,
                                           @RequestParam String billingCycle,
                                           @RequestParam boolean vat
    ) throws Exception {

        billingCycle = billingCycle.toUpperCase();
        PlanDefinition plan = service.getPlan(planCode);
        boolean annual = "ANNUAL".equalsIgnoreCase(billingCycle);
        String duration = annual ? "ANNUAL" : "MONTHLY";
        String desc = "Piano " + planCode + (annual ? " (Annuale -20%)" : " (Mensile)");
        description = desc;
        // Importo: se usi piani/abbonamenti PayPal, qui useresti i planId.
        // In modalità 'one-shot' usa l'importo derivato dal tuo listino:
        long amountdb = annual ? plan.getPriceAnnualCents() : plan.getPriceMonthlyCents();
        amountMinor = amountdb;
        //amountMinor è il prezzo in centesimi annuale o mensile.
        Long price = calculateAmount(amountMinor, duration);
        amountMinor = price;
        if(!annual)
            amountMinor +=1;
        String value = String.format(Locale.US, "%.2f", amountMinor / 100.0);
        AmountWithBreakdown amount = new AmountWithBreakdown().currencyCode(currency).value(value);
        PurchaseUnitRequest unit = new PurchaseUnitRequest()
                .description(description)
                .amountWithBreakdown(amount)
                .customId(tenantId + "|" + planCode + "|" + billingCycle + "|" + plan.getId())// utile ritrovarlo in webhook/capture
                .invoiceId(UUID.randomUUID().toString()); // idempotenza “light”

        OrderRequest orderReq = new OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(unit))
                .applicationContext(new ApplicationContext()
                        .returnUrl(props.getPaypal().getSuccessUrl())
                        .cancelUrl(props.getPaypal().getCancelUrl())
                        .brandName("VeriCert")
                        .landingPage("NO_PREFERENCE")
                        .userAction("PAY_NOW"));

        OrdersCreateRequest req = new OrdersCreateRequest()
                .prefer("return=representation")
                .requestBody(orderReq);

        var response = factory.client().execute(req);
        var order = response.result(); // id es: "5O190127TN364715T"

        // Salva pagamento PENDING
        Payment p = new Payment();
        p.setTenantId(tenantId);
        p.setCertificateId(certificateId);
        p.setProvider("PAYPAL");
        p.setProviderIntentId(order.id()); // <--- orderId
        p.setStatus("PENDING");
        p.setAmountMinor(amountMinor);
        p.setCurrency(currency);
        p.setDescription(description);
        p.setIdempotencyKey(null); // opzionale
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        payRepo.save(p);

        // Link di approvazione
        String approveUrl = order.links().stream()
                .filter(l -> "approve".equalsIgnoreCase(l.rel())).findFirst()
                .map(LinkDescription::href).orElseThrow();

        return Map.of(
                "orderId", order.id(),
                "approveUrl", approveUrl
        );
    }
    // Calcola l'importo da pagare'
    private Long calculateAmount(Long amountMinor, String plan) {
        BigDecimal vat;
        switch (plan){
           case "MONTHLY":
                   //Calcolo l'iva
                   vat = BigDecimal.valueOf((amountMinor * 22) / 100);
                   vat = BigDecimal.valueOf(Math.round(vat.doubleValue()));
                   amountMinor = Math.round(amountMinor.doubleValue() + vat.doubleValue());
                   break;
            case "ANNUAL":
                //Calcolo l'iva
                vat = BigDecimal.valueOf((amountMinor * 22) / 100);
                vat = BigDecimal.valueOf(Math.round(vat.doubleValue()));
                amountMinor = (Math.round(amountMinor.doubleValue() + vat.doubleValue())) * 12;
                break;
               default:
                   throw new IllegalArgumentException("Plan non supportato");

       }
        return amountMinor;
    }
    @PostMapping("/capture/{orderId}")
    public ResponseEntity<?> capture(@PathVariable String orderId) throws Exception {

        OrdersGetRequest getReq = new OrdersGetRequest(orderId);
        var getRes = factory.client().execute(getReq);
        var orderDetails = getRes.result();

        // Estrai customId dal primo purchase unit
        String customId = orderDetails.purchaseUnits().get(0).customId();
        String[] info = customId.split("\\|"); // Escape della pipe!

        String tenantId = info[0];
        String planCode = info[1];
        String cycle = info[2];
        String planDefId = info[3];

        OrdersCaptureRequest req = new OrdersCaptureRequest(orderId);
        req.requestBody(new OrderRequest()); // body vuoto

        var res = factory.client().execute(req);
        var order = res.result();

        if ("COMPLETED".equalsIgnoreCase(order.status())) {
            payRepo.findByProviderIntentId(orderId).ifPresent(p -> {
                // Persisti attivazione
                service.activatePlan(Long.valueOf(tenantId), planCode, cycle, planDefId, "PAYPAL");
                p.setStatus("SUCCEEDED");
                p.setUpdatedAt(Instant.now());
                payRepo.save(p);
                // TODO: qui puoi emettere/sbloccare certificato se p.getCertificateId()!=null
            });
            return ResponseEntity.ok(Map.of("status","SUCCEEDED"));
        } else {
            payRepo.findByProviderIntentId(orderId).ifPresent(p -> {
                p.setStatus("FAILED");
                p.setUpdatedAt(Instant.now());
                payRepo.save(p);
            });
            return ResponseEntity.status(400).body(Map.of("status", order.status()));
        }
    }
}
