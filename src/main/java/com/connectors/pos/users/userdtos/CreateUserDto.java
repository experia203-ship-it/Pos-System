package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.*;

public record CreateUserDto(

        @NotBlank(message = "name is required")
        @Size(max = 50 ,message = "name must not exceed 50 chars")
      String name,

        @NotBlank(message = "email is required")
        @Email(message = "enter a correct email")
        String email ,

        @NotBlank(message = "password is required")
        @Pattern(regexp ="^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$" ,
                message = "Password must be at least 8 characters long, contain one uppercase letter, one lowercase letter, one digit, and one special character with no spaces.")
        String password


) {
}
