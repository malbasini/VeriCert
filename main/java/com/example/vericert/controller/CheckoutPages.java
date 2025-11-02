package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class CheckoutPages {
    @GetMapping("/checkout/success")
    public String ok(@RequestParam("session_id") String sessionId, Model model) throws Exception {
        var session = com.stripe.model.checkout.Session.retrieve(sessionId);
        model.addAttribute("amount", session.getAmountTotal());
        model.addAttribute("currency", session.getCurrency());
        return "payments/checkout/success";
    }
    @GetMapping("/checkout/cancel")
    public String cancel() { return "payments/checkout/cancel"; }

}