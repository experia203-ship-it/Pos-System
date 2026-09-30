package com.connectors.pos.users;

import com.connectors.pos.security.JwtService;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserLoginDto;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AuthControllerMvcTest {
    private final UserService userService = mock(UserService.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new AuthController(userService, authenticationManager, jwtService)).build();

    @Test
    void validLoginSetsHttpOnlyJwtCookieAndRedirectsToLayout() throws Exception {
        UserDetails principal = User.withUsername("person@example.com").password("hash").authorities("ROLE_USER").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("signed-token");

        mvc.perform(post("/auth/login").param("email", "person@example.com").param("password", "Pass1234!"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: empty"))
                .andExpect(header().string("HX-Redirect", "/layout"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("jwt=signed-token"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("Path=/"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"),
                                org.hamcrest.Matchers.containsString("Max-Age=86400"))));
    }

    @Test
    void invalidLoginFormDoesNotAuthenticateOrCreateCookie() throws Exception {
        mvc.perform(post("/auth/login").param("email", "not-an-email").param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: auth-error"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeExists("errorMessage"));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void registrationRedirectsAfterServiceCreatesUser() throws Exception {
        mvc.perform(post("/auth/register").param("name", "Sam").param("email", "sam@example.com")
                        .param("password", "Pass1234!"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: empty"))
                .andExpect(header().string("HX-Redirect", "/auth/login"));

        verify(userService).createUser(new CreateUserDto("Sam", "sam@example.com", "Pass1234!"));
    }

    @Test
    void logoutClearsSecurityContextAndExpiresJwtCookie() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("person@example.com", "ignored"));

        mvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: empty"))
                .andExpect(header().string("HX-Redirect", "/auth/login"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("jwt="),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("Max-Age=0"))));

        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }
}
