package com.connectors.pos.settings;

import com.connectors.pos.users.RoleRepository;
import com.connectors.pos.users.Roles;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Roles adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN is missing"));
        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is missing"));

        Users demoUser = userRepository.findByEmail("user123@Gmail.com").orElse(null);
        if (demoUser != null) {
            demoUser.getRoles().add(adminRole);
            demoUser.getRoles().add(userRole);
            userRepository.save(demoUser);
            return;
        }

        if (userRepository.existsByName("user")) {
            throw new IllegalStateException("The demo username is already assigned to another account");
        }

        demoUser = Users.builder()
                .name("user")
                .email("user123@Gmail.com")
                .password(passwordEncoder.encode("A@123456"))
                .roles(new java.util.HashSet<>(java.util.Set.of(adminRole, userRole)))
                .build();
        userRepository.save(demoUser);
    }
}
