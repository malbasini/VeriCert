package com.example.vericert.controller;


import com.example.vericert.dto.ContactFormDto;
import com.example.vericert.service.ContactEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ContactSupportController {

    private final ContactEmailService mailService;

    public ContactSupportController(ContactEmailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/contact")
    public String contactForm(Model model) {
        model.addAttribute("form", new ContactFormDto());
        return "public/contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("form") ContactFormDto form,
                                BindingResult br,
                                HttpServletRequest request,
                                Model model) {

        if (br.hasErrors()) {
            return "public/contact";
        }

        String ip = extractClientIp(request);
        String ua = request.getHeader("User-Agent");

        mailService.sendContactRequest(form, ip, ua);

        model.addAttribute("ok", true);
        model.addAttribute("message", "Messaggio inviato correttamente. Ti risponderemo al più presto.");
        model.addAttribute("flashMsg", "Messaggio inviato correttamente. Ti risponderemo al più presto.");
        model.addAttribute("flashOk",true);
        model.addAttribute("form", new ContactFormDto()); // reset form
        return "public/contact";
    }

    private static String extractClientIp(HttpServletRequest request) {
        // se hai reverse proxy, usa X-Forwarded-For (configurato correttamente)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
