package com.connectors.pos.settings;

import com.connectors.pos.settings.settingsdtos.SettingsMapper;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final SettingsRepository settingsRepo;
    private final SettingsMapper settingMapper;

    @Transactional
    @CacheEvict(value = "settings",allEntries = true)
    public void updateGlobalSettings(SettingsUpdateDto update) {

        Settings settings = settingsRepo.findById(1)
                .orElseGet(() -> {
                    return Settings.builder().id(1).build();
                });

        settingMapper.updateEntity(update, settings);
        try {
            if (update.logoFile() != null && !update.logoFile().isEmpty()) {
                settings.setLogo(update.logoFile().getBytes());
            }
            // If logoFile is empty, we do nothing. The existing database logo remains intact.
        } catch (IOException e) {
            throw new RuntimeException("Failed to process logo file", e);
        }
        settingsRepo.save(settings);
    }

    @Cacheable("settings")
    public SettingsResponseDto getSettings() {

        Settings settings = settingsRepo.findById(1).
                orElseGet(() -> {
                    return Settings.builder().id(1).currencySymbol("EGP").theme(Theme.SYSTEM_DEFAULT).address("31 st").posStyle(PosStyle.HORIZONTAL).shiftManagement(true).build();
                });

        return settingMapper.toResponse(settings);

    }
}