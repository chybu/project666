package demo.app;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import demo.user.User;
import demo.user.UserRepository;

@Controller
public class AppController {

    private final UserRepository userRepository;

    public AppController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    // GET
    @GetMapping("")
    public String home_page(){
        return "index";
    }
    
    @GetMapping("/register")
    public String user_registration_page(Model model){
        model.addAttribute("user", new User());
        return "registration_form";
    }

    @GetMapping("/users")
    public String user_list_page(){
        return "users";
    }

    // POST
    @PostMapping("/process_register")
    public String register_user(User user){
        userRepository.save(user);
        return "successful_user_registration";
    }

}
