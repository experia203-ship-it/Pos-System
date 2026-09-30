package com.connectors.pos.users;

import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.userdtos.ResetPasswordDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserService userServo;

    @GetMapping
    public String viewUsersPage(Model model) {
        model.addAttribute("usersList", userServo.getAllUsers());
        return "users";
    }

    @PostMapping("/{id}/promote")
    public String promoteUser(@PathVariable Long id, Model model) {
        userServo.promoteToAdmin(id);
        model.addAttribute("usersList", userServo.getAllUsers());
        return "users :: users-table";
    }

    @PostMapping("/{id}/demote")
    public String demoteUser(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        userServo.demoteFromAdmin(id, principal.getId());
        model.addAttribute("usersList", userServo.getAllUsers());
        return "users :: users-table";
    }

    @GetMapping("/{id}/reset-password")
    public String showResetPasswordDialog(@PathVariable Long id, Model model) {
        model.addAttribute("targetUser", userServo.getUserSummary(id));
        model.addAttribute("resetPasswordForm", new ResetPasswordDto("", ""));
        return "users :: reset-password-dialog";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                 @Valid @ModelAttribute("resetPasswordForm") ResetPasswordDto dto,
                                 BindingResult bindingResult,
                                 HttpServletResponse response,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Retarget", "#reset-password-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", bindingResult.getFieldError().getDefaultMessage());
            return "fragments/auth-messages :: exceptions-response";
        }
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Retarget", "#reset-password-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "fragments/auth-messages :: exceptions-response";
        }

        userServo.resetPasswordByAdmin(id, dto.newPassword());
        model.addAttribute("usersList", userServo.getAllUsers());
        response.setHeader("HX-Trigger", "close-modal");
        return "users :: users-table";
    }
}
