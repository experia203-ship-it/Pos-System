package com.connectors.pos.users.userdtos;

public record UserListDto(
        Long id,
        String name,
        String email,
        boolean admin,
        boolean protectedAccount
) {
}
