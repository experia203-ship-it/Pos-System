package com.connectors.pos.settings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class LicenseCheckInterceptor implements HandlerInterceptor {

    private final SettingsRepository settingsRepo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/auth/login") || uri.startsWith("/auth/register")
                || uri.startsWith("/css") || uri.startsWith("/js")
                || uri.startsWith("/images") || uri.startsWith("/auth/trial-expired")) {
            return true;
        }

        Settings settings = settingsRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException("App settings not initialized"));

        // 1. If they bought the full software, bypass all trial checks
        if (settings.isLicensed()) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. Anti-Clock-Tampering Check: Did they turn back the Windows clock?
        if (settings.getLastAccessedAt() != null && now.isBefore(settings.getLastAccessedAt())) {
            redirectToLockout(request, response, "Clock tampering detected.");
            return false;
        }

        // 3. Expiration Check
        if (settings.getTrialEndsAt() != null && now.isAfter(settings.getTrialEndsAt())) {
            redirectToLockout(request, response, "Trial expired.");
            return false;
        }

        // 4. Update the watermark for clock tampering
        settings.setLastAccessedAt(now);
        settingsRepo.save(settings);

        return true;
    }

    private void redirectToLockout(HttpServletRequest req, HttpServletResponse res, String reason) throws IOException, IOException {
        if ("true".equals(req.getHeader("HX-Request"))) {
            res.setHeader("HX-Redirect", "/auth/trial-expired");
        } else {
            res.sendRedirect("/auth/trial-expired");
        }
    }
}