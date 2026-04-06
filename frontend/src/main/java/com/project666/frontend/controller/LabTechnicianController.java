package com.project666.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
@RequestMapping("/labtechnician")
@PreAuthorize("hasRole('LAB_TECHNICIAN')")
public class LabTechnicianController {

}
