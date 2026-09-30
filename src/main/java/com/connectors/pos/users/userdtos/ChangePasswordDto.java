package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordDto(

        @NotBlank(message = "current password is required")
        String currentPassword,

        @NotBlank(message = "new password is required")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
                message = "Password must be at least 8 characters long, contain one uppercase letter, one lowercase letter, one digit, and one special character with no spaces.")
        String newPassword,

        @NotBlank(message = "please confirm the new password")
        String confirmPassword

) {
}
