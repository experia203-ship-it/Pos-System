package com.connectors.pos.settings;

import com.connectors.pos.users.DefaultAdmin;
import com.connectors.pos.users.RoleRepository;
import com.connectors.pos.users.Roles;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!demo")   // your online demo server keeps using DemoInitializer
public class FirstRunAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public FirstRunAdminInitializer(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // only runs on a brand-new database
        }

        Roles adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN is missing"));
        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is missing"));

        Users admin = Users.builder()
                .name(DefaultAdmin.NAME)
                .email(DefaultAdmin.EMAIL)
                .password(passwordEncoder.encode(DefaultAdmin.PASSWORD))
                .mustChangePassword(true)
                .roles(new HashSet<>(Set.of(adminRole, userRole)))
                .build();
        userRepository.save(admin);
    }
}