package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Vercert | Certificati digitali verificabili con QR code per aziende");
        model.addAttribute("description", "Crea, gestisci e verifica certificati digitali con QR code. Vercert aiuta le aziende italiane a garantire autenticità, tracciabilità e controllo.");
        return "index";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Vercert | Certificati digitali verificabili con QR per aziende");
        model.addAttribute("description", "Privacy su Vercert: certificati digitali, verifica con QR code, revoca, privacy e uso aziendale.");
        return "public/privacy";
    }


    @GetMapping("/docs")
    public String docs(Model model) {
        model.addAttribute("title", "DOCS Vercert | Certificati digitali verificabili con QR per aziende");
        model.addAttribute("description", "Documentazione su Vercert: certificati digitali, verifica con QR code, revoca, privacy e uso aziendale.");
        return "public/docs";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("title", "FAQ Vercert | Certificati digitali verificabili con QR per aziende");
        model.addAttribute("description", "Risposte alle domande frequenti su Vercert: certificati digitali, verifica con QR code, revoca, privacy e uso aziendale.");
        return "public/faq";
    }

    @GetMapping("/come-funziona")
    public String comeFunziona(Model model) {
        model.addAttribute("title", "Come funziona Vercert | Certificati digitali verificabili per aziende");
        model.addAttribute("description", "Scopri come funziona Vercert: crea, gestisci e verifica certificati digitali con QR code in modo sicuro per aziende in Italia.");
        return "public/come-funziona";
    }
    @GetMapping("/firma-digitale")
    public String firmaDigitale(Model model) {
        model.addAttribute("title", "Firma digitale Vercert | Certificati digitali verificabili per aziende");
        model.addAttribute("description", "Firma digitale Ververt: crea, gestisci e verifica certificati digitali con QR code in modo sicuro per aziende in Italia.");
        return "public/firma-digitale";
    }





}

