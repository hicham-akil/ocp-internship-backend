package org.example.project_stage_backend.controller;

import org.example.project_stage_backend.dto.*;
import org.example.project_stage_backend.service.IndicateurService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestController {

    private final IndicateurService indicateurService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/complet")
    public ResponseEntity<IndicateursDTO> ingestComplet(@RequestBody DonneesCompleteDTO dto) {
        log.info("POST /ingest/complet reçu — date={}", dto.getAnalyseGypse().getDate());

        // ✅ Broadcast INPUT immédiatement en temps réel
        messagingTemplate.convertAndSend("/topic/input/gypse",      dto.getAnalyseGypse());
        messagingTemplate.convertAndSend("/topic/input/phosphate",   dto.getAnalysePhosphate());
        messagingTemplate.convertAndSend("/topic/input/production",  dto.getProduction());
        messagingTemplate.convertAndSend("/topic/input/consommation",dto.getConsommation());

        // ✅ Traitement + sauvegarde DB + broadcast OUTPUT via IndicateurService
        IndicateursDTO result = indicateurService.ingestDonneesCompletes(dto);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/gypse")
    public ResponseEntity<IndicateursDTO> ingestGypse(@RequestBody AnalyseGypseDTO dto) {
        messagingTemplate.convertAndSend("/topic/input/gypse", dto);
        IndicateursDTO result = indicateurService.ingestGypse(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/phosphate")
    public ResponseEntity<IndicateursDTO> ingestPhosphate(@RequestBody AnalysePhosphateDTO dto) {
        messagingTemplate.convertAndSend("/topic/input/phosphate", dto);
        IndicateursDTO result = indicateurService.ingestPhosphate(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/production")
    public ResponseEntity<IndicateursDTO> ingestProduction(@RequestBody ProductionDTO dto) {
        messagingTemplate.convertAndSend("/topic/input/production", dto);
        IndicateursDTO result = indicateurService.ingestProduction(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/consommation")
    public ResponseEntity<IndicateursDTO> ingestConsommation(@RequestBody ConsommationDTO dto) {
        messagingTemplate.convertAndSend("/topic/input/consommation", dto);
        IndicateursDTO result = indicateurService.ingestConsommation(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/perte")
    public ResponseEntity<IndicateursDTO> ingestPerte(@RequestBody PerteDTO dto) {
        IndicateursDTO result = indicateurService.ingestPerte(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @DeleteMapping("/perte/{id}")
    public ResponseEntity<Void> deletePerte(@PathVariable Long id) {
        log.info("DELETE /ingest/perte/{} reçu", id);
        indicateurService.supprimerPerte(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET endpoints (inchangés) ──────────────────────────────
    @GetMapping("/gypse/dernier")
    public ResponseEntity<AnalyseGypseDTO> getDernierGypse() {
        return indicateurService.getDernierGypse()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/gypse/historique")
    public ResponseEntity<List<AnalyseGypseDTO>> getHistoriqueGypse() {
        return ResponseEntity.ok(indicateurService.getHistoriqueGypse());
    }

    @GetMapping("/perte/dernier")
    public ResponseEntity<PerteDTO> getDernierPerte() {
        return indicateurService.getDernierPerte()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/perte/historique")
    public ResponseEntity<List<PerteDTO>> getHistoriquePerte() {
        return ResponseEntity.ok(indicateurService.getHistoriquePerte());
    }

    @GetMapping("/phosphate/dernier")
    public ResponseEntity<AnalysePhosphateDTO> getDernierPhosphate() {
        return indicateurService.getDernierPhosphate()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/phosphate/historique")
    public ResponseEntity<List<AnalysePhosphateDTO>> getHistoriquePhosphate() {
        return ResponseEntity.ok(indicateurService.getHistoriquePhosphate());
    }

    @GetMapping("/production/dernier")
    public ResponseEntity<ProductionDTO> getDerniereProduction() {
        return indicateurService.getDerniereProduction()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/production/historique")
    public ResponseEntity<List<ProductionDTO>> getHistoriqueProduction() {
        return ResponseEntity.ok(indicateurService.getHistoriqueProduction());
    }
}