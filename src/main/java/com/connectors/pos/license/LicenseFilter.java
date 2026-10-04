package com.connectors.pos.license;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Sends every request to the activation page until this PC has a valid license. */
public class LicenseFilter extends OncePerRequestFilter {

    private static final String ACTIVATE_PAGE = "/license/activate";

    private final LicenseService licenseService;

    public LicenseFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (licenseService.isLicensed() || isAllowed(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", ACTIVATE_PAGE);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendRedirect(ACTIVATE_PAGE);
        }
    }

    private boolean isAllowed(String path) {
        return path.startsWith("/license/")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }
}
