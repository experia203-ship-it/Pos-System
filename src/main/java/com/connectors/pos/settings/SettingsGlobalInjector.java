package com.connectors.pos.settings;

import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@ControllerAdvice
public class SettingsGlobalInjector {
    private final SettingsService settingsServo;

    @ModelAttribute("globalSettings")
    public SettingsResponseDto getSettings(){

        return settingsServo.getSettings();
    }
}
