package demo.controller.client;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import demo.domain.entities.RoleEnum;
import demo.exception.UnknowRoleException;

@Controller
public class WebController {

    @GetMapping("/")
    public String home(){
        return "index";
    }

    @GetMapping("/callback")
    public String callback(
        Authentication authentication
    ){
        RoleEnum role = getUserRole(authentication);
        if (RoleEnum.PATIENT.equals(role)){
            return "redirect:/patient/homepage";
        }
        else if(RoleEnum.DOCTOR.equals(role)){
            return "redirect:/doctor/homepage";
        }
        else if(RoleEnum.RECEPTIONIST.equals(role)){
            return "redirect:/receptionist/homepage";
        }

        return "redirect:/error";
        
    }

    @GetMapping("/error")
    public String error(){
        return "error";
    }

    private RoleEnum getUserRole(Authentication authentication){
        String role = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .filter(auth -> auth
                .startsWith("ROLE_")
            )
            .findFirst()
            .orElse(null);
        
        role = role.replace("ROLE_", "");
        try {
            return RoleEnum.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new UnknowRoleException();
        }
    }
}
