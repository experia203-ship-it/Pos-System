package com.connectors.pos.settings;

import com.connectors.pos.ordersystem.LayoutController;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class LayoutHomeControllerMvcTest {

    @Test
    void rootRedirectsToLogin() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HomeController()).build();

        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/auth/login"));
    }

    @Test
    void layoutSelectsTemplateFromConfiguredPosStyle() throws Exception {
        SettingsGlobalInjector injector = mock(SettingsGlobalInjector.class);
        when(injector.getSettings()).thenReturn(settings(PosStyle.HORIZONTAL), settings(PosStyle.VERTICAL));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LayoutController(injector)).build();

        mvc.perform(get("/layout"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/layout"));
        mvc.perform(get("/layout"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/layout-custom"));
    }

    private static SettingsResponseDto settings(PosStyle style) {
        return new SettingsResponseDto(null, null, null, null, Theme.SYSTEM_DEFAULT,
                null, "EGP", style, null, true);
    }
}
