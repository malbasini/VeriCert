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

        mailService.sendToSupportFromUser(
                "Contatti — " + form.getSubject(),
                html,
                form.getEmail()
        );

        ra.addFlashAttribute("contactSent", true);
        return "redirect:/contact";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
