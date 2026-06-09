package org.example.project_stage_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.project_stage_backend.entity.Seuil;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeuilDTO {
    private Long id;
    private String code;
    private String label;
    private Seuil.Type type;
    private Double warning;
    private Double critique;
    private LocalDateTime updatedAt;

    public static SeuilDTO from(Seuil seuil) {
        return SeuilDTO.builder()
                .id(seuil.getId())
                .code(seuil.getCode())
                .label(seuil.getLabel())
                .type(seuil.getType())
                .warning(seuil.getWarning())
                .critique(seuil.getCritique())
                .updatedAt(seuil.getUpdatedAt())
                .build();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private Double warning;

        @NotNull(message = "Le seuil critique est obligatoire")
        private Double critique;
    }
}
