package com.connectors.pos.users;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/auth/login";
    }
}
