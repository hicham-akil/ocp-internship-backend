package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "indicateurs_calcules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_indicateurs_date", columnNames = "date")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IndicateursCalcules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Date de référence — clé commune avec les 4 tables sources.
     * ATTENTION : tolérance de ±1 minute appliquée lors de la jointure
     * dans IndicateurService pour éviter les conflits de timing.
     */
    @Column(nullable = false)
    private LocalDateTime date;




    // ── Rendement de Concentration (RC) ───────────────────────────
    // RC = (P2O5_produite / P2O5_entrant_dans_phosphate) × 100
    @Column(name = "rc") private Double rc;

    // ── Rendement d'Incorporation (RI) ────────────────────────────
    // RI = (P2O5_dans_acide / P2O5_dans_phosphate) × 100
    @Column(name = "ri") private Double ri;

    // ── Capacité de production (CAP) ──────────────────────────────
    // CAP = Q_P2O5_total produit (T/j)
    @Column(name = "cap") private Double cap;

    // ── Consommations spécifiques ─────────────────────────────────
    // Conso H2SO4 par tonne de P2O5 produite (T H2SO4 / T P2O5)
    @Column(name = "conso_h2so4")      private Double consoH2so4;
    // Conso eau brute par tonne de P2O5 produite (m³ / T P2O5)
    @Column(name = "conso_eau_brute")  private Double consoEauBrute;
    // Conso phosphates par tonne de P2O5 produite (T P / T P2O5)
    @Column(name = "conso_phosphates") private Double consoPhosphates;
    // Conso vapeur par tonne de P2O5 produite (T / T P2O5)
    @Column(name = "conso_vapeur")     private Double consoVapeur;
}
