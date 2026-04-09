package com.project666.frontend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.project666.backend.domain.entity.RoleEnum;
import com.project666.frontend.security.SecurityUtil;

import com.project666.frontend.service.FrontenndUserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final FrontenndUserService userService;

    @GetMapping("/")
    public String home(){
        return "forward:/landingPage/index.html";
    }

    @GetMapping("/callback")
    public String callback(
        Authentication authentication
    ){

        if (authentication.getPrincipal() instanceof OidcUser oidcUser){
            userService.provisionUser(oidcUser);
        }

        RoleEnum role = SecurityUtil.getUserRole(authentication);
        if (RoleEnum.PATIENT.equals(role)){
            return "redirect:/patient/dashboard/home";
        }
        else if(RoleEnum.DOCTOR.equals(role)){
            return "redirect:/doctor/dashboard/home";
        }
        else if(RoleEnum.RECEPTIONIST.equals(role)){
            return "redirect:/receptionist/dashboard/home";
        }
        else if(RoleEnum.NURSE.equals(role)){
            return "redirect:/nurse/dashboard/home";
        }
        else if(RoleEnum.ACCOUNTANT.equals(role)){
            return "redirect:/accountant/dashboard/home";
        }

        return "redirect:/error";
        
    }
}