package com.connectors.pos.settings;

import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
@RequestMapping("/settings")
public class SettingsController {
    private final SettingsService settingsServo;
    private final SettingsRepository settingsRepo;

    @GetMapping

    public String viewSettingsPage(Model model) {

        SettingsResponseDto response = settingsServo.getSettings();
        model.addAttribute("settings", response);

        return "settings";
    }

    @PostMapping
    public String updateSettings(@Valid @ModelAttribute("settings") SettingsUpdateDto update, BindingResult bindResult, HttpServletResponse response) {

        if (bindResult.hasErrors()) {
            response.setHeader("HX-Retarget", "#settings-div");
            response.setHeader("HX-Reswap", "innerHTML");
            return "settings :: settings-fragment";
        }

        settingsServo.updateGlobalSettings(update);
        response.setHeader("HX-Refresh", "true");
        return "fragments/layout :: main-window";
    }


    @GetMapping("/logo")
    public ResponseEntity<byte[]> getCompanyLogo() {
        // Fetch your single settings record (adjust based on your actual service)
        SettingsResponseDto settings = settingsServo.getSettings();

        if (settings == null || settings.logo() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(settings.logo());
    }
}

