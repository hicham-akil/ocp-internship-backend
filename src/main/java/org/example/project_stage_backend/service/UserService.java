package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.CreateUserRequest;
import org.example.project_stage_backend.dto.UserResponse;
import org.example.project_stage_backend.entity.User;
import org.example.project_stage_backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();
        if (request.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Seuls les roles LABO et VIEWER peuvent etre attribues");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur existe deja");
        }

        User user = userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build());

        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
