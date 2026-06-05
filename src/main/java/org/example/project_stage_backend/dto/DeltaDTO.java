package org.example.project_stage_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeltaDTO {
    private Double rc;
    private Double ri;
    private Double cap;
    private Double consoH2so4;
    private Double consoEauBrute;
    private Double consoPhosphates;
    private Double consoVapeur;
}
