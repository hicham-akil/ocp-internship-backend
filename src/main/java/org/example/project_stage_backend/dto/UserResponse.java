package org.example.project_stage_backend.dto;

import org.example.project_stage_backend.entity.User;

public record UserResponse(Long id, String username, User.Role role) {
}
