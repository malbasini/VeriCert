package com.example.vericert.controller;

import com.example.vericert.domain.Payment;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.PlanEnforcementService;
import com.paypal.orders.OrdersCaptureRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/paypal")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class PaypalPages {

    private final PaymentRepository payRepo;
    private final PlanEnforcementService planEnforcementService;
    private final TenantSettingsRepository tenantSettingsRepo;

    public PaypalPages(PaymentRepository payRepo,
                         PlanEnforcementService planEnforcementService,
                         TenantSettingsRepository tenantSettingsRepo) {
        this.payRepo = payRepo;
        this.planEnforcementService = planEnforcementService;
        this.tenantSettingsRepo = tenantSettingsRepo;

    }


    @GetMapping("/success")
    public String order(@RequestParam("token") String orderId,
                          @RequestParam(value="PayerID", required=false) String payerId,
                          Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("payerId", payerId);
        return "paypal/order";
    }
    @GetMapping("/cancel")
    public String cancelPaypal() { return "paypal/cancel"; }

    @GetMapping("success/{orderId}")
    public String success(@PathVariable String orderId,Model model) {
        Payment p = payRepo.findByProviderIntentId(orderId).get();
        // se vuoi, recuperi info da Stripe/PayPal qui e calcoli:
        // - provider
        // - amountFormatted
        // - nextRenewalDate
        // - transactionId
        // - currentPlan (CurrentPlanView)
        Instant date = tenantSettingsRepo.findByTenantId(p.getTenantId()).get().getCurrentPeriodEnd();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(date);
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(p.getTenantId());
        model.addAttribute("currentPlan", currentPlan);
        model.addAttribute("amountFormatted",formatCents(p.getAmountMinor()));
        model.addAttribute("provider", "PayPal"); // o "PayPal"
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", orderId); // o l’id PayPal/Stripe reale
        return "paypal/success";
    }

    public static String formatCents(long cents) {
        BigDecimal eur = BigDecimal.valueOf(cents).movePointLeft(2); // divide per 100 senza perdita
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return fmt.format(eur); // es. 2990 -> "€ 29,90"
    }


}