package org.example.project_stage_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.project_stage_backend.entity.User;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Le nom d'utilisateur contient des caracteres invalides")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caracteres")
    private String password;

    @NotNull(message = "Le role est obligatoire")
    private User.Role role;
}
