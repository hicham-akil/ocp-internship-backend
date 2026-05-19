package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.AuthDTO;
import org.example.project_stage_backend.entity.User;
import org.example.project_stage_backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthDTO.LoginResponse(token, user.getRole().name(), user.getUsername());
    }
}