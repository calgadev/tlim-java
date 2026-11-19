package com.tlim.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the admin account from TLIM_ADMIN_USERNAME / TLIM_ADMIN_PASSWORD on startup, so a fresh
 * database is usable without editing the users table by hand.
 * Does nothing when either value is blank. An existing user with that name is promoted to ADMIN;
 * its password is never changed.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${tlim.admin.username:}") String username,
                          @Value("${tlim.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isEmpty() || password.isEmpty()) return;

        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            if (!"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                log.info("Promoted existing user '{}' to ADMIN", username);
            }
        }, () -> {
            if (password.length() < MIN_PASSWORD_LENGTH) {
                log.warn("Admin account not created: TLIM_ADMIN_PASSWORD must be at least {} characters",
                        MIN_PASSWORD_LENGTH);
                return;
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Created admin account '{}'", username);
        });
    }
}
