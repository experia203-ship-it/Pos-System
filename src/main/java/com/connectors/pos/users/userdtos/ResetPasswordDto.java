package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordDto(

        @NotBlank(message = "{validation.common.password.required}")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
                message = "{validation.common.password.complexity}")
        String newPassword,

        @NotBlank(message = "{validation.common.password.confirmRequired}")
        String confirmPassword

) {
}
