package com.connectors.pos.users;

import com.connectors.pos.exceptions.RoleNotFoundException;
import com.connectors.pos.exceptions.UserEmailAlreadyExistsException;
import com.connectors.pos.exceptions.UserNameAlreadyExistsException;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserMapper;
import com.connectors.pos.users.userdtos.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsAdminRoleToFirstUserAndEncodesPassword() {
        CreateUserDto request = new CreateUserDto("Sam", "sam@example.com", "Pass1234!");
        Roles role = Roles.builder().id(3L).name("ROLE_ADMIN").build();
        Users mapped = Users.builder().name("Sam").email("sam@example.com").roles(new HashSet<>()).build();
        Users saved = Users.builder().id(11L).name("Sam").email("sam@example.com").password("encoded").build();
        UserResponseDto expected = new UserResponseDto("Sam", "encoded");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByName(request.name())).thenReturn(false);
        when(roleRepository.lockInitialAdministratorRegistration()).thenReturn(1);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userRepository.count()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(mapped)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        assertSame(expected, userService.createUser(request));

        org.junit.jupiter.api.Assertions.assertEquals("encoded", mapped.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(mapped.getRoles().contains(role));
        verify(userRepository).save(mapped);
    }

    @Test
    void rejectsDuplicateEmailBeforeCheckingNameOrCreatingUser() {
        CreateUserDto request = new CreateUserDto("Sam", "sam@example.com", "Pass1234!");
        when(roleRepository.lockInitialAdministratorRegistration()).thenReturn(1);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserEmailAlreadyExistsException.class, () -> userService.createUser(request));

        verify(userRepository, never()).existsByName(any());
        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateName() {
        CreateUserDto request = new CreateUserDto("Sam", "sam@example.com", "Pass1234!");
        when(roleRepository.lockInitialAdministratorRegistration()).thenReturn(1);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByName(request.name())).thenReturn(true);

        assertThrows(UserNameAlreadyExistsException.class, () -> userService.createUser(request));

        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotPersistFirstUserWhenAdminRoleIsMissing() {
        CreateUserDto request = new CreateUserDto("Sam", "sam@example.com", "Pass1234!");
        Users mapped = Users.builder().roles(new HashSet<>()).build();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByName(request.name())).thenReturn(false);
        when(roleRepository.lockInitialAdministratorRegistration()).thenReturn(1);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userRepository.count()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> userService.createUser(request));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void blocksConcurrentUnauthenticatedRegistrationAfterInitialAccountExists() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonymous", null));
        when(roleRepository.lockInitialAdministratorRegistration()).thenReturn(1);
        when(userRepository.existsByEmail("next@example.com")).thenReturn(false);
        when(userRepository.existsByName("Next")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> userService.createUser(new CreateUserDto("Next", "next@example.com", "Pass1234!")));
        verify(userRepository, never()).save(any());
    }
}
