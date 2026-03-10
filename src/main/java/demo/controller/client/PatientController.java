package demo.controller.client;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient")
public class PatientController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientDashboard(){
        return "patient";
    }
}
