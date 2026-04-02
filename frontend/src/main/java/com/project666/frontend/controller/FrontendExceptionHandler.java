package com.project666.frontend.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.project666.frontend.security.SecurityUtil;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

@ControllerAdvice(basePackages = "com.project666.frontend")
public class FrontendExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Authentication authentication) {
        String target = "redirect:/";
        if (authentication != null && authentication.isAuthenticated()) {
            target = "redirect:" + SecurityUtil.getDashboardHomePath(authentication);
        }
        return target;
    }

    // This is for internal error handling. So if any unexpeted error happen, this will handle
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model){
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}
