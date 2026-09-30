package com.connectors.pos.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.same;

class JwtSecurityBehaviorTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
    private final JwtFilter filter = new JwtFilter(jwtService, detailsService);
    private final UserDetails user = User.withUsername("person@example.com")
            .password("encoded").authorities("ROLE_USER").build();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signedTokenRoundTripsAndRejectsDifferentPrincipalOrTampering() {
        JwtService realJwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(realJwtService, "secretKey", secret);
        realJwtService.init();
        UserDetails rightfulUser = User.withUsername("person@example.com").password("hash").authorities("ROLE_USER").build();
        UserDetails otherUser = User.withUsername("other@example.com").password("hash").authorities("ROLE_USER").build();

        String token = realJwtService.generateToken(rightfulUser);

        assertEquals("person@example.com", realJwtService.getUsernameFromToken(token));
        assertTrue(realJwtService.validateToken(token, rightfulUser));
        assertFalse(realJwtService.validateToken(token, otherUser));
        assertFalse(realJwtService.validateToken(token + "x", rightfulUser));
    }

    @Test
    void acceptsValidJwtCookieAndSetsAuthenticatedPrincipal() throws Exception {
        when(jwtService.getUsernameFromToken("signed")).thenReturn("person@example.com");
        when(detailsService.loadUserByUsername("person@example.com")).thenReturn(user);
        when(jwtService.validateToken("signed", user)).thenReturn(true);
        MockHttpServletRequest request = requestWithJwt("signed", "/private");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals("person@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("ROLE_USER",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void missingJwtCookiePassesRequestThroughWithoutAuthenticationLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(detailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidJwtDoesNotAuthenticateRequest() throws Exception {
        when(jwtService.getUsernameFromToken("signed")).thenReturn("person@example.com");
        when(detailsService.loadUserByUsername("person@example.com")).thenReturn(user);
        when(jwtService.validateToken("signed", user)).thenReturn(false);

        filter.doFilter(requestWithJwt("signed", "/private"), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deletedAccountInJwtDoesNotBreakRequestProcessing() throws Exception {
        when(jwtService.getUsernameFromToken("signed")).thenReturn("deleted@example.com");
        when(detailsService.loadUserByUsername("deleted@example.com"))
                .thenThrow(new UsernameNotFoundException("account was deleted"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithJwt("signed", "/private"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(chain.getRequest() != null);
    }

    @Test
    void authRoutesAreExcludedFromJwtCookieProcessing() throws Exception {
        MockHttpServletRequest request = requestWithJwt("signed", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(jwtService, never()).getUsernameFromToken(org.mockito.ArgumentMatchers.any());
        verify(chain).doFilter(same(request), same(response));
    }

    private MockHttpServletRequest requestWithJwt(String value, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setCookies(new jakarta.servlet.http.Cookie("jwt", value));
        return request;
    }
}
