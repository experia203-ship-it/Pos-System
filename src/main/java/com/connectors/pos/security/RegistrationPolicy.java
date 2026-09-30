package com.connectors.pos.security;

import com.connectors.pos.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("registrationPolicy")
@RequiredArgsConstructor
public class RegistrationPolicy {

    private final UserRepository userRepository;

    public boolean canRegister(Authentication authentication) {
        if (userRepository.count() == 0) {
            return true;
        }

        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
