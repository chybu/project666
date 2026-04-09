package com.project666.frontend.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/accountant")
@PreAuthorize("hasRole('ACCOUNTANT')")
public class AccountantController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

    @GetMapping("/homepage")
    public String redirectHomepage() {
        return "redirect:/accountant/dashboard/home";
    }

    @GetMapping("/dashboard/home")
    public String home(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient
    ) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);

        User user = userRepository.findById(userId)
            .orElseThrow();

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        return "accountant/dashboard/home";
    }

    @GetMapping("/dashboard/finances")
    public String finances() {
        return "accountant/dashboard/finances";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "accountant/dashboard/notifications";
    }

    @GetMapping("/dashboard/profile")
    public String profile(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        Model model
    ){
        UUID userId = OidcUserUtil.getUserId(oidcUser);

        User user = userRepository.findById(userId)
            .orElseThrow();

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        model.addAttribute("user", user);

        return "accountant/dashboard/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(
        @AuthenticationPrincipal OidcUser oidcUser
    ){
        UUID userId = OidcUserUtil.getUserId(oidcUser);

        keycloakService.deleteUser(userId);
        userRepository.deleteById(userId);

        return "redirect:/logout";
    }

    @GetMapping("/profile/keycloak")
    public String redirectToKeycloakAccount() {
        return keycloakService.getAccountRedirect();
    }

    @GetMapping("/profile/keycloak-password")
    public String redirectToKeycloakPassword() {
        return keycloakService.getPasswordRedirect();
    }
}