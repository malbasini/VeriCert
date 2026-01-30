package com.example.vericert.controller;

import com.example.vericert.dto.ContactForm;
import com.example.vericert.service.MailService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.example.vericert.controller.BillingController.formatEuroIT;


@Controller
public class ContactController {

    private final MailService mailService;

    public ContactController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/contact")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ContactForm());
        }
        model.addAttribute("title", "Contatti Vercert | Certificati digitali verificabili per aziende");
        model.addAttribute("description", "Contati Vercert: crea, gestisci e verifica certificati digitali con QR code in modo sicuro per aziende in Italia.");
        return "public/contact";
    }

    @PostMapping("/contact")
    public String submit(@Valid @ModelAttribute("form") ContactForm form,
                         BindingResult br,
                         Principal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.form", br);
            ra.addFlashAttribute("form", form);
            return "redirect:/contact";
        }

        String user = principal != null ? principal.getName() : "unknown";
        String html = """
      <h3>Nuovo messaggio Contatti</h3>
      <p><b>Account:</b> %s</p>
      <p><b>Email contatto:</b> %s</p>
      <p><b>Oggetto:</b> %s</p>
      <p><b>Messaggio:</b><br/>%s</p>
      """.formatted(esc(user), esc(form.getEmail()), esc(form.getSubject()),
                esc(form.getMessage()).replace("\n","<br/>"));
        //mailService.sendToSupportFromUser(
        //        "Contatti — " + form.getSubject(),
        //        html,
       //         form.getEmail()
        //);
        send();
        ra.addFlashAttribute("contactSent", true);
        return "redirect:/contact";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private void send() {

        Map<String, Object> vars = new HashMap<>();
        vars.put("action", "Acquisto"); // oppure "Rinnovo" se lo usi anche per quello
        vars.put("provider", "PayPal");
        vars.put("customer_name", "Mario Rossi"); // o nome utente, se ce l’hai
        vars.put("paid_at", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Rome")).format(Instant.now()));
        vars.put("payment_ref", "I-Y6HB1V7PFPEV");
        vars.put("plan_name", "PRO");
        vars.put("billing_cycle", "MONTHLY");
        vars.put("subscription_id", "I-Y6HB1V7PFPEV");
        vars.put("amount_net", formatEuroIT("0.50"));
        vars.put("vat_amount", formatEuroIT("0.11"));
        vars.put("amount_total", formatEuroIT("0.61"));
        vars.put("portal_url", "https://app.vercert.org/");
        vars.put("support_email", "support@app.vercert.org");
        vars.put("company_name", "VeriCert");
        vars.put("company_address", "…");

        mailService.sendPurchaseSuccess(
                "malbasini@gmail.com",                      // email cliente
                "Conferma pagamento - " + vars.getOrDefault("plan_name","PRO"),
                vars
        );
    }
}
