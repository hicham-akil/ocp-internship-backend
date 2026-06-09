package org.example.project_stage_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.SeuilDTO;
import org.example.project_stage_backend.service.SeuilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seuils")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeuilController {

    private final SeuilService seuilService;

    @GetMapping
    public ResponseEntity<List<SeuilDTO>> getSeuils() {
        return ResponseEntity.ok(seuilService.getAll());
    }

    @PutMapping("/{code}")
    public ResponseEntity<SeuilDTO> updateSeuil(
            @PathVariable String code,
            @Valid @RequestBody SeuilDTO.UpdateRequest request) {
        return ResponseEntity.ok(seuilService.update(code, request));
    }
}
