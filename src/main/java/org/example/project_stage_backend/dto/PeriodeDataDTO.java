package org.example.project_stage_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodeDataDTO {
    private String label;
    private LocalDateTime debut;
    private LocalDateTime fin;
    private List<IndicateurPointDTO> points;
    private IndicateurStatsDTO stats;
}
