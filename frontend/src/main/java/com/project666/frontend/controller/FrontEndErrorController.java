package com.project666.frontend.controller;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.project666.frontend.security.SecurityUtil;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FrontEndErrorController implements ErrorController{

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Authentication authentication, org.springframework.ui.Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // User typed /error directly, not a real error dispatch
        if (statusAttr == null) {
            if (authentication != null && authentication.isAuthenticated()) {
                return "redirect:" + SecurityUtil.getDashboardHomePath(authentication);
            }
            return "redirect:/";
        }

        Object exceptionAttr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        Integer status = null;
        if (statusAttr != null) {
            status = Integer.parseInt(statusAttr.toString());
        }

        if (status != null && status == 404) {
            if (authentication != null && authentication.isAuthenticated()) {
                return "redirect:" + SecurityUtil.getDashboardHomePath(authentication);
            }
            return "redirect:/";
        }

        String errorMessage = null;

        if (exceptionAttr instanceof Throwable throwable && throwable.getMessage() != null) {
            errorMessage = throwable.getMessage();
        } else if (messageAttr != null) {
            errorMessage = messageAttr.toString();
        } else {
            errorMessage = "An unexpected error occurred.";
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}

