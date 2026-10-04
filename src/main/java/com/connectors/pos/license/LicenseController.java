package com.connectors.pos.license;

import com.connectors.pos.i18n.Messages;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/license")
@ConditionalOnProperty(name = "app.license.enabled", havingValue = "true")
public class LicenseController {

    private final LicenseService licenseService;

    @Value("${app.license.support-whatsapp:}")
    private String supportWhatsapp;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping("/activate")
    public String showActivationPage(Model model) {
        if (licenseService.isLicensed()) {
            return "redirect:/auth/login";
        }
        String code = licenseService.getDeviceCode();
        model.addAttribute("deviceCode", code);
        model.addAttribute("whatsappUrl", whatsappLink(code));
        return "license-activate";
    }

    @PostMapping("/activate")
    public String activate(@RequestParam("key") String key, HttpServletResponse response, Model model) {
        if (licenseService.activate(key)) {
            response.setHeader("HX-Redirect", "/auth/login");
            return "fragments/auth-messages :: empty";
        }
        // htmx only swaps 2xx responses, so answer 200 with the message to show.
        response.setStatus(HttpServletResponse.SC_OK);
        model.addAttribute("errorMessage", Messages.get("license.invalid"));
        return "fragments/auth-messages :: login-error";
    }

    private String whatsappLink(String code) {
        String number = supportWhatsapp == null ? "" : supportWhatsapp.replaceAll("[^0-9]", "");
        if (number.isEmpty()) {
            return null;
        }
        String text = URLEncoder.encode(Messages.get("license.whatsapp.text", code), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://wa.me/" + number + "?text=" + text;
    }
}
