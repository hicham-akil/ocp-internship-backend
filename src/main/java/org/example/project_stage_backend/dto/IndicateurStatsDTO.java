package org.example.project_stage_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicateurStatsDTO {
    private StatsDTO rc;
    private StatsDTO ri;
    private StatsDTO cap;
    private StatsDTO consoH2so4;
    private StatsDTO consoEauBrute;
    private StatsDTO consoPhosphates;
    private StatsDTO consoVapeur;
}
