package com.connectors.pos.users;


import com.connectors.pos.security.JwtService;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserLoginDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userServo;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

 @GetMapping("/register")

public String showRegisterPage(Model model){
model.addAttribute("userForm",new CreateUserDto("","",""));

        return "register";
    }

    @PostMapping("/register")

    public String processRegistration(@Valid @ModelAttribute("userForm") CreateUserDto dto,
                                      BindingResult bindingResult, Model model, HttpServletResponse response){

if(bindingResult.hasErrors()) {
    String defaultMessage = bindingResult.getFieldError().getDefaultMessage();

    model.addAttribute("errorMessage", defaultMessage);


    return "fragments/auth-messages :: auth-error";


}

   userServo.createUser(dto);
response.setHeader("HX-Redirect","/auth/login");
 return "fragments/auth-messages :: empty";
 }



    @GetMapping("/login")

    public String showLoginPage(Model model){
model.addAttribute("loginForm",new UserLoginDto("",""));
        return "login";
    }

    @GetMapping("/trial-expired")
    public String showTrialExpiredPage() {
        return "trial-expired";
    }

    @PostMapping("/login")

    public String logIn(@Valid @ModelAttribute("loginForm") UserLoginDto dto ,
                        BindingResult bindingResult , Model model, HttpServletResponse response){
   if(bindingResult.hasErrors()){
       model.addAttribute("errorMessage",bindingResult.getFieldError().getDefaultMessage());
       return "fragments/auth-messages :: auth-error";
   }

   Authentication authenticated = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(),dto.password()));


   UserDetails user = (UserDetails) authenticated.getPrincipal();
   String token=jwtService.generateToken(user);
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(86400);
        response.addCookie(jwtCookie);
  response.setHeader("HX-Redirect","/layout");
     return "fragments/auth-messages :: empty";
    }


    @PostMapping("/logout")
    public String logOut(HttpServletResponse response){
        SecurityContextHolder.clearContext();

        Cookie jwtCookie = new Cookie("jwt",null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        response.setHeader("HX-Redirect","/auth/login");

        return "fragments/auth-messages :: empty" ;
    }
}
