package com.connectors.pos.settings.settingsdtos;

import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.PrintSize;
import com.connectors.pos.settings.Theme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record SettingsUpdateDto(
        @NotBlank(message="please provide your company name")
        @Size(max=255,message="this name exceeds the allowed size")
        String companyName,
        @Size(max=13 , message="please provide a valid Egyptian phone number")
        @Pattern(regexp = "^01[0125]\\d{8}$",message="please provide a valid Egyptian phone number")
        String phoneNumber,
        String address,
        String taxRegistrationNumber,
        @NotNull(message="theme is required")
        Theme theme,

        PrintSize printSize,
        @Size(max=50)
        String currencySymbol,
        PosStyle posStyle,
        MultipartFile logoFile,
        boolean shiftManagement

) {
}
