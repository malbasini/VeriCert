package com.example.vericert.controller;

import com.example.vericert.util.AuthUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.extras.springsecurity6.auth.AuthUtils;

@Controller
@RequestMapping("/download")
public class AdminPageDownload {
    @GetMapping()
    public String usageDetail(Model model)
    {
        Long tenantId = AuthUtil.currentTenantId();
        model.addAttribute("pageTitle", "Download Files");
        model.addAttribute("active", "files");
        model.addAttribute("tenantId", tenantId);
        return "downloads/download"; // creeremo questo template
    }

}
