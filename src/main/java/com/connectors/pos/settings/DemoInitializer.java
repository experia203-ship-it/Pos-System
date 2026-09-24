package com.connectors.pos.settings;

import com.connectors.pos.users.RoleRepository;
import com.connectors.pos.users.Roles;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class DemoInitializer implements CommandLineRunner {

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoInitializer(SettingsRepository settingsRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        Settings settings = settingsRepository.findById(1).orElseGet(Settings::new);

        if (settings.getTrialEndsAt() == null) {
            settings.setLicensed(false);
            settings.setTrialEndsAt(now.plusDays(3));
            settings.setLastAccessedAt(now);
            settingsRepository.save(settings);
        }

        if (!userRepository.existsByEmail("user123@Gmail.com")
                && !userRepository.existsByName("user")) {
            Roles userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER is missing"));

            Users demoUser = Users.builder()
                    .name("user")
                    .email("user123@Gmail.com")
                    .password(passwordEncoder.encode("A@123456"))
                    .roles(new java.util.HashSet<>(java.util.Set.of(userRole)))
                    .build();
            userRepository.save(demoUser);
        }
    }
}
