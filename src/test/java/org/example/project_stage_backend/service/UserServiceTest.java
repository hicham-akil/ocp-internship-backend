package org.example.project_stage_backend.service;

import org.example.project_stage_backend.dto.CreateUserRequest;
import org.example.project_stage_backend.dto.UserResponse;
import org.example.project_stage_backend.entity.User;
import org.example.project_stage_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(userRepository, passwordEncoder);

    @Test
    void createUserHashesPasswordAndReturnsNoSensitiveData() {
        CreateUserRequest request = request("new.viewer", "password123", User.Role.VIEWER);
        when(userRepository.existsByUsername("new.viewer")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response).isEqualTo(new UserResponse(42L, "new.viewer", User.Role.VIEWER));
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                passwordEncoder.matches("password123", user.getPasswordHash())));
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        CreateUserRequest request = request("existing", "password123", User.Role.LABO);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe deja");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsAdminRole() {
        CreateUserRequest request = request("other.admin", "password123", User.Role.ADMIN);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LABO et VIEWER");
        verify(userRepository, never()).save(any());
    }

    private CreateUserRequest request(String username, String password, User.Role role) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }
}
