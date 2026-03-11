package com.hospital.portal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortalController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/doctor")
    public String doctor(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("name", user.getFullName() != null ? user.getFullName() : user.getPreferredUsername());
        return "doctor";
    }

    @GetMapping("/patient")
    public String patient(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("name", user.getFullName() != null ? user.getFullName() : user.getPreferredUsername());
        return "patient";
    }

    @GetMapping("/receptionist")
    public String receptionist(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("name", user.getFullName() != null ? user.getFullName() : user.getPreferredUsername());
        return "receptionist";
    }
}
