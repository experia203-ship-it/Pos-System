package com.connectors.pos.users;


import com.connectors.pos.exceptions.UserManagementException;
import com.connectors.pos.i18n.Messages;
import com.connectors.pos.security.JwtService;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.userdtos.ChangePasswordDto;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserLoginDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
@RequiredArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userServo;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookies;

    @Value("${app.demo.credentials-enabled:false}")
    private boolean demoCredentialsEnabled;

 @PreAuthorize("@registrationPolicy.canRegister(authentication)")
 @GetMapping("/register")

public String showRegisterPage(Model model){
model.addAttribute("userForm",new CreateUserDto("","",""));

        return "register";
    }

    @PreAuthorize("@registrationPolicy.canRegister(authentication)")
    @PostMapping("/register")

    public String processRegistration(@Valid @ModelAttribute("userForm") CreateUserDto dto,
                                      BindingResult bindingResult, Model model, HttpServletResponse response,
                                      Authentication authentication){

if(bindingResult.hasErrors()) {
    String defaultMessage = bindingResult.getFieldError().getDefaultMessage();

    model.addAttribute("errorMessage", defaultMessage);


    return "fragments/auth-messages :: auth-error";


}

   userServo.createUser(dto);
response.setHeader("HX-Redirect", authentication != null
        && authentication.getPrincipal() instanceof com.connectors.pos.security.UserPrincipal
        ? "/layout" : "/auth/login");
 return "fragments/auth-messages :: empty";
 }



    @GetMapping("/login")

    public String showLoginPage(Model model){
        String email = "";
        String password = "";
        if (demoCredentialsEnabled) {
            email = "user123@Gmail.com";
            password = "A@123456";
        } else if (userServo.isDefaultAdminPending()) {
            // Fresh local install: the default admin still has its default password.
            email = DefaultAdmin.EMAIL;
            password = DefaultAdmin.PASSWORD;
        }
        model.addAttribute("loginForm",new UserLoginDto("",""));
        model.addAttribute("demoCredentialsEnabled",demoCredentialsEnabled);
        model.addAttribute("prefillEmail", email);
        model.addAttribute("prefillPassword", password);
        return "login";
    }

    @GetMapping("/force-password-change")
    @PreAuthorize("isAuthenticated()")
    public String showForcePasswordChange(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (principal == null || !principal.isMustChangePassword()) {
            return "redirect:/layout";
        }
        boolean defaultAdmin = DefaultAdmin.EMAIL.equalsIgnoreCase(principal.getUsername());
        model.addAttribute("defaultAdmin", defaultAdmin);
        model.addAttribute("currentPassword", defaultAdmin ? DefaultAdmin.PASSWORD : "");
        return "force-password-change";
    }

    @PostMapping("/login")

    public String logIn(@Valid @ModelAttribute("loginForm") UserLoginDto dto ,
                        BindingResult bindingResult , Model model, HttpServletResponse response){
   if(bindingResult.hasErrors()){
       model.addAttribute("errorMessage",bindingResult.getFieldError().getDefaultMessage());
       return "fragments/auth-messages :: auth-error";
   }

   Authentication authenticated;
   try {
       authenticated = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(),dto.password()));
   } catch (AuthenticationException ex) {
       // htmx only swaps 2xx responses, so answer 200 with the message to show under the form.
       response.setStatus(HttpServletResponse.SC_OK);
       model.addAttribute("errorMessage", Messages.get("error.login.invalidCredentials"));
       return "fragments/auth-messages :: login-error";
   }


   UserDetails user = (UserDetails) authenticated.getPrincipal();
   String token=jwtService.generateToken(user);
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
  response.setHeader("HX-Redirect","/layout");
     return "fragments/auth-messages :: empty";
    }


    @PostMapping("/logout")
    public String logOut(HttpServletResponse response){
        SecurityContextHolder.clearContext();

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        response.setHeader("HX-Redirect","/auth/login");

        return "fragments/auth-messages :: empty" ;
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserPrincipal principal,
                                 HttpServletResponse response,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Retarget", "#password-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", bindingResult.getFieldError().getDefaultMessage());
            return "fragments/auth-messages :: exceptions-response";
        }
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Retarget", "#password-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "fragments/auth-messages :: exceptions-response";
        }

        try {
            userServo.changeOwnPassword(principal.getId(), dto.currentPassword(), dto.newPassword());
        } catch (UserManagementException ex) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Retarget", "#password-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", ex.getMessage());
            return "fragments/auth-messages :: exceptions-response";
        }
        if (principal.isMustChangePassword()) {
            // First-time password change: continue into the app.
            response.setHeader("HX-Redirect", "/layout");
        }
        model.addAttribute("successMessage", "Your password was changed successfully.");
        return "fragments/auth-messages :: inline-success";
    }
}
