package org.example.project_stage_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparaisonDTO {
    private PeriodeDataDTO periode1;
    private PeriodeDataDTO periode2;
    private DeltaDTO delta;
    private Long alertPeriode1;
    private Long alertPeriode2;
}
