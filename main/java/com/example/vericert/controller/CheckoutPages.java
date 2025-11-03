package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
public class CheckoutPages {
    @GetMapping("/checkout/success")
    public String ok(@RequestParam("session_id") String sessionId, Model model) throws Exception {
        var session = com.stripe.model.checkout.Session.retrieve(sessionId);
        model.addAttribute("amount", formatCents(session.getAmountTotal()));
        model.addAttribute("currency", session.getCurrency());
        return "stripe/success";
    }
    @GetMapping("/checkout/cancel")
    public String cancel() { return "stripe/cancel"; }


    public static String formatCents(long cents) {
        BigDecimal eur = BigDecimal.valueOf(cents).movePointLeft(2); // divide per 100 senza perdita
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return fmt.format(eur); // es. 2990 -> "€ 29,90"
    }


}

