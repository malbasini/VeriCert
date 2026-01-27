package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vf/invoices")
public class PublicInvoicePageController {

    @GetMapping("/{publicCode}")
    public String page(@PathVariable String publicCode, Model model) {
        model.addAttribute("publicCode", publicCode);
        model.addAttribute("title", "Verifica fatture");
        model.addAttribute("description", "Verifica fatture");
        return "verification/invoice-verify";
    }
}