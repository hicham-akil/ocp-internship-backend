package org.example.project_stage_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerteDTO {
    private Long id;
    private LocalDateTime date;
    private Double se;
    private Double syn;
    private Double intVal;
}
