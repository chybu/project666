package demo.controller.client;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorDashboard(){
        return "doctor";
    }
}
