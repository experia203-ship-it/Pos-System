package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDto(

        @Email
        @NotBlank(message ="email is required")
        String email,

        @NotBlank(message = "password is required")
        String password


) {
}
