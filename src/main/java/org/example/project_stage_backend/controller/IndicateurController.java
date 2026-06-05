package org.example.project_stage_backend.controller;

import org.example.project_stage_backend.dto.ComparaisonDTO;
import org.example.project_stage_backend.dto.IndicateursDTO;
import org.example.project_stage_backend.service.IndicateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/indicateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IndicateurController {

    private final IndicateurService indicateurService;

    @GetMapping("/derniers")
    public ResponseEntity<IndicateursDTO> getDernierIndicateur() {
        return indicateurService.getDernierIndicateur()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/periode")
    public ResponseEntity<List<IndicateursDTO>> getIndicateursSurPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        if (debut.isAfter(fin)) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(indicateurService.getIndicateursSurPeriode(debut, fin));
    }

    @GetMapping("/historique")
    public ResponseEntity<List<IndicateursDTO>> getHistoriqueRecent() {
        return ResponseEntity.ok(indicateurService.getHistoriqueRecent());
    }

    @GetMapping("/comparaison")
    public ResponseEntity<ComparaisonDTO> comparerPeriodes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut1,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin1,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut2,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin2) {

        return ResponseEntity.ok(indicateurService.comparerPeriodes(debut1, fin1, debut2, fin2));
    }
}