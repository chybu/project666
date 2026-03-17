package com.project666.frontend.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "com.project666.frontend")
public class FrontendExceptionHandler {
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model){
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}
