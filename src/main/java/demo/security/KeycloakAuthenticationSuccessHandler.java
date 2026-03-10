package demo.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import demo.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeycloakAuthenticationSuccessHandler implements AuthenticationSuccessHandler{

    private final UserService userService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        if (
            authentication!=null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof Jwt jwt
        ){
            userService.provisionUser(jwt);
        }
        
    }
}
