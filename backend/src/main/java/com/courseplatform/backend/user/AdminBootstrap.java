package com.courseplatform.backend.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String email;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.enabled}") boolean enabled,
            @Value("${app.bootstrap-admin.username}") String username,
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.email}") String email
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.findByUsername(username).isPresent()) {
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters when bootstrap is enabled");
        }
        userRepository.create(
                username.trim(), passwordEncoder.encode(password),
                email == null || email.isBlank() ? null : email.trim().toLowerCase(), UserRole.ADMIN
        );
        log.info("Bootstrap administrator created: {}", username);
    }
}
