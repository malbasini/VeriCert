package com.example.vericert.controller;

import com.example.vericert.domain.Payment;
import com.example.vericert.repo.PaymentRepository;
import com.paypal.orders.OrdersCaptureRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/paypal")
public class PaypalPages {

    private final PaymentRepository payRepo;

    public PaypalPages(PaymentRepository payRepo) {
        this.payRepo = payRepo;
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
        model.addAttribute("amount",p.getAmountMinor());
        model.addAttribute("currency",p.getCurrency());
        return "paypal/success";
    }

}