package com.connectors.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sends a logged-in user whose account is flagged mustChangePassword to the
 * "set a new password" page until they have changed it.
 * Deliberately not a @Component: it is added to the security chain by hand,
 * so Spring Boot does not also register it as a second servlet filter.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final String CHANGE_PAGE = "/auth/force-password-change";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword()
                && !isAllowed(request.getRequestURI())) {

            if ("true".equals(request.getHeader("HX-Request"))) {
                response.setHeader("HX-Redirect", CHANGE_PAGE);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.sendRedirect(CHANGE_PAGE);
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(String path) {
        return path.equals(CHANGE_PAGE)
                || path.equals("/auth/change-password")
                || path.equals("/auth/logout")
                || path.equals("/error")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }
}
