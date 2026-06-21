package org.example.project_stage_backend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project_stage_backend.entity.User;
import org.example.project_stage_backend.repository.UserRepository;
import org.example.project_stage_backend.service.SeuilService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SeuilService seuilService;

    @Override
    public void run(String... args) {
        createIfAbsent("admin",  "admin1234",  User.Role.ADMIN);
        createIfAbsent("labo",   "labo1234",   User.Role.LABO);
        createIfAbsent("viewer", "viewer1234", User.Role.VIEWER);
        seuilService.initialiserDefaults();
    }

    private void createIfAbsent(String username, String password, User.Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .build());
            log.info("User créé : {} ({})", username, role);
        }
    }
}
