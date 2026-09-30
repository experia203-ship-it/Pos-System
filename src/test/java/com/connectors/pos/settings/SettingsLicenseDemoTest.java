package com.connectors.pos.settings;

import com.connectors.pos.settings.settingsdtos.SettingsMapper;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import com.connectors.pos.users.RoleRepository;
import com.connectors.pos.users.Roles;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsLicenseDemoTest {

    @Test
    void settingsFallbackBuildsDocumentedDefaultsWithoutPersisting() {
        SettingsRepository repository = mock(SettingsRepository.class);
        SettingsMapper mapper = mock(SettingsMapper.class);
        SettingsResponseDto response = new SettingsResponseDto("Company", null, "31 st", null,
                Theme.SYSTEM_DEFAULT, PrintSize.A5, "EGP", PosStyle.HORIZONTAL, null, true);
        when(repository.findById(1)).thenReturn(Optional.empty());
        when(mapper.toResponse(any(Settings.class))).thenReturn(response);

        SettingsResponseDto result = new SettingsService(repository, mapper).getSettings();

        assertSame(response, result);
        org.mockito.ArgumentCaptor<Settings> captor = org.mockito.ArgumentCaptor.forClass(Settings.class);
        verify(mapper).toResponse(captor.capture());
        Settings fallback = captor.getValue();
        assertTrue(fallback.isShiftManagement());
        assertSame(Theme.SYSTEM_DEFAULT, fallback.getTheme());
        assertSame(PosStyle.HORIZONTAL, fallback.getPosStyle());
        org.junit.jupiter.api.Assertions.assertEquals("EGP", fallback.getCurrencySymbol());
        org.junit.jupiter.api.Assertions.assertEquals("31 st", fallback.getAddress());
        verify(repository, never()).save(any());
    }

    @Test
    void settingsUpdateSavesUploadedLogoAndLeavesLogoAloneWhenUploadIsEmpty() {
        SettingsRepository repository = mock(SettingsRepository.class);
        SettingsMapper mapper = mock(SettingsMapper.class);
        Settings existing = Settings.builder().id(1).logo(new byte[]{1, 2}).build();
        when(repository.findById(1)).thenReturn(Optional.of(existing));
        SettingsService service = new SettingsService(repository, mapper);
        SettingsUpdateDto updateWithLogo = update(new MockMultipartFile("logoFile", "logo.png",
                "image/png", new byte[]{9, 8}));
        service.updateGlobalSettings(updateWithLogo);
        assertArrayEquals(new byte[]{9, 8}, existing.getLogo());
        verify(repository).save(existing);

        SettingsUpdateDto updateWithoutLogo = update(new MockMultipartFile("logoFile", "", "image/png", new byte[0]));
        service.updateGlobalSettings(updateWithoutLogo);
        assertArrayEquals(new byte[]{9, 8}, existing.getLogo());
        verify(repository, org.mockito.Mockito.times(2)).save(existing);
        verify(mapper, org.mockito.Mockito.times(2)).updateEntity(any(), org.mockito.ArgumentMatchers.same(existing));
    }

    @Test
    void demoInitializerSeedsRoleBackedDemoAccountOnlyWhenAbsent() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Roles role = Roles.builder().id(7L).name("ROLE_USER").build();
        Roles adminRole = Roles.builder().id(8L).name("ROLE_ADMIN").build();
        when(users.findByEmail("user123@Gmail.com")).thenReturn(Optional.empty());
        when(users.existsByName("user")).thenReturn(false);
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(encoder.encode("A@123456")).thenReturn("encoded-password");
        DemoInitializer initializer = new DemoInitializer(users, roles, encoder);

        initializer.run();

        org.mockito.ArgumentCaptor<Users> captor = org.mockito.ArgumentCaptor.forClass(Users.class);
        verify(users).save(captor.capture());
        Users demoUser = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("user123@Gmail.com", demoUser.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("encoded-password", demoUser.getPassword());
        assertTrue(demoUser.getRoles().contains(role));
        assertTrue(demoUser.getRoles().contains(adminRole));
        verify(users).save(demoUser);
    }

    @Test
    void demoResetDoesNotRunOutsideRenderEvenWhenEnabled() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        DemoDataReset reset = new DemoDataReset(dataSource);
        ReflectionTestUtils.setField(reset, "isAutoResetEnabled", true);

        reset.executeDatabaseReset();

        verify(dataSource, never()).getConnection();
    }

    private SettingsUpdateDto update(MockMultipartFile logo) {
        return new SettingsUpdateDto("Company", null, "Address", null, Theme.DARK,
                PrintSize.A4, "USD", PosStyle.VERTICAL, logo, false);
    }
}
