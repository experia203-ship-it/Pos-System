package com.connectors.pos.security;

import com.connectors.pos.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationPolicyTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RegistrationPolicy policy = new RegistrationPolicy(userRepository);

    @Test
    void allowsInitialSetupWhenNoUsersExist() {
        when(userRepository.count()).thenReturn(0L);

        assertThat(policy.canRegister(null)).isTrue();
    }

    @Test
    void onlyAllowsAdministratorRegistrationAfterInitialSetup() {
        when(userRepository.count()).thenReturn(1L);
        var cashier = UsernamePasswordAuthenticationToken.authenticated(
                "cashier", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var administrator = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(policy.canRegister(cashier)).isFalse();
        assertThat(policy.canRegister(administrator)).isTrue();
    }
}
