package com.example.vericert.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping("/home")
    public String showHomePage(Model model) {
        // Ritorna la vista
        model.addAttribute("code", "RRP7W3RZVSUG27J4E54K9R8R");
        return "home";
    }
    @GetMapping("/certificati")
    public String insertCertificate() {
        // Ritorna la vista
        return "certificates/certificate";
    }
    @GetMapping("/revoke")
    public String revokeCertificate() {
        // Ritorna la vista
        return "certificates/revoke";
    }
}
