package com.connectors.pos.settings.settingsdtos;

import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.PrintSize;
import com.connectors.pos.settings.Theme;

public record SettingsResponseDto(
        String companyName,
        String phoneNumber,
        String address,
        String taxRegistrationNumber,
        Theme theme,
        PrintSize printSize,
        String currencySymbol,
        PosStyle posStyle,
        byte[] logo,
        boolean shiftManagement

) {
}
