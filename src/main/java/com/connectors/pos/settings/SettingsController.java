package com.connectors.pos.settings;

import com.connectors.pos.backup.BackupService;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor
@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {
    private final SettingsService settingsServo;
    private final SettingsRepository settingsRepo;
    private final BackupService backupService;

    @GetMapping

    public String viewSettingsPage(Model model) {

        SettingsResponseDto response = settingsServo.getSettings();
        model.addAttribute("settings", response);
        model.addAttribute("backupSupported", backupService.isBackupSupported());

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


    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String viewPreferencesPage(Model model) {

        SettingsResponseDto response = settingsServo.getSettings();
        model.addAttribute("settings", response);
        model.addAttribute("changePasswordForm", new com.connectors.pos.users.userdtos.ChangePasswordDto("", "", ""));

        return "fragments/preferences :: preferences-fragment";
    }

    @PostMapping("/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String updatePreferences(@RequestParam("theme") com.connectors.pos.settings.Theme theme, HttpServletResponse response) {

        settingsServo.updateTheme(theme);
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

    @GetMapping("/backup/download")
    public ResponseEntity<Resource> downloadBackup() {
        Path backup = backupService.createBackup();
        Resource resource = new FileSystemResource(backup);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(backup.toFile().length())
                .body(resource);
    }
}
