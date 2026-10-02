package com.connectors.pos.users.userdtos;

import jakarta.validation.constraints.*;

public record CreateUserDto(

        @NotBlank(message = "{validation.common.name.required}")
        @Size(max = 50 ,message = "{validation.user.name.size}")
      String name,

        @NotBlank(message = "{validation.common.email.required}")
        @Email(message = "{validation.user.email.invalid}")
        String email ,

        @NotBlank(message = "{validation.common.password.required}")
        @Pattern(regexp ="^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$" ,
                message = "{validation.common.password.complexity}")
        String password


) {
}
