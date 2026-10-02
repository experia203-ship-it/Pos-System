package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDto(

        @Email
        @NotBlank(message ="{validation.common.email.required}")
        String email,

        @NotBlank(message = "{validation.common.password.required}")
        String password


) {
}
