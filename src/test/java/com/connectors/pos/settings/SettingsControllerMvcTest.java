package com.connectors.pos.settings;

import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class SettingsControllerMvcTest {
    private final SettingsService service = mock(SettingsService.class);
    private final SettingsRepository repository = mock(SettingsRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SettingsController(service, repository))
            .setViewResolvers((viewName, locale) -> (model, request, response) -> { })
            .build();

    @Test
    void settingsPageAddsCurrentSettingsToModel() throws Exception {
        SettingsResponseDto settings = settings(null);
        when(service.getSettings()).thenReturn(settings);

        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("settings", settings));
    }

    @Test
    void validUpdatePersistsSettingsAndRequestsRefresh() throws Exception {
        mvc.perform(post("/settings").param("companyName", "Garage").param("phoneNumber", "01012345678")
                        .param("theme", "DARK").param("currencySymbol", "EGP")
                        .param("posStyle", "HORIZONTAL").param("shiftManagement", "true")
                        .param("taxRate", "15.50"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/layout :: main-window"))
                .andExpect(header().string("HX-Refresh", "true"));

        org.mockito.ArgumentCaptor<SettingsUpdateDto> update =
                org.mockito.ArgumentCaptor.forClass(SettingsUpdateDto.class);
        verify(service).updateGlobalSettings(update.capture());
        assertEquals(0, update.getValue().taxRate().compareTo(new java.math.BigDecimal("15.50")));
    }

    @Test
    void invalidUpdateReturnsFragmentAndRetargetHeaders() throws Exception {
        mvc.perform(post("/settings").param("companyName", "").param("theme", "DARK"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings :: settings-fragment"))
                .andExpect(header().string("HX-Retarget", "#settings-div"))
                .andExpect(header().string("HX-Reswap", "innerHTML"));

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void rejectsTaxRateAboveOneHundredPercent() throws Exception {
        mvc.perform(post("/settings").param("companyName", "Garage").param("theme", "DARK")
                        .param("taxRate", "100.01"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings :: settings-fragment"))
                .andExpect(header().string("HX-Retarget", "#settings-div"));

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void logoReturnsNotFoundWhenNotConfiguredAndPngWhenPresent() throws Exception {
        when(service.getSettings()).thenReturn(settings(null), settings(new byte[]{1, 2, 3}));

        mvc.perform(get("/settings/logo"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/settings/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    private static SettingsResponseDto settings(byte[] logo) {
        return new SettingsResponseDto("Garage", "01012345678", "Cairo", null, Theme.DARK,
                PrintSize.THERMAL, "EGP", PosStyle.HORIZONTAL, logo, true);
    }
}
