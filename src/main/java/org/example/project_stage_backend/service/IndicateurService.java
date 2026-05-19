package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project_stage_backend.dto.*;
import org.example.project_stage_backend.entity.*;
import org.example.project_stage_backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicateurService {

    private static final long TOLERANCE_MINUTES = 1L;

    private final AlerteService                 alerteService;
    private final PredictionService             predictionService;
    private final AnalyseGypseRepository        gypseRepo;
    private final AnalysePhosphateRepository    phosphateRepo;
    private final ProductionRepository          productionRepo;
    private final ConsommationRepository        consommationRepo;
    private final PerteRepository               perteRepo;
    private final IndicateursCalculesRepository indicateursRepo;
    private final SimpMessagingTemplate         messagingTemplate;

    // ── INGESTION ─────────────────────────────────────────────────

    @Transactional
    public IndicateursDTO ingestDonneesCompletes(DonneesCompleteDTO dto) {
        sauvegarderGypse(dto.getAnalyseGypse());
        sauvegarderPhosphate(dto.getAnalysePhosphate());
        sauvegarderProduction(dto.getProduction());
        sauvegarderConsommation(dto.getConsommation());
        return recalculerDepuisDate(dto.getAnalyseGypse().getDate());
    }

    @Transactional
    public IndicateursDTO ingestPerte(PerteDTO dto) {
        Perte perte = perteRepo.save(Perte.builder()
                .date(dto.getDate())
                .se(dto.getSe())
                .syn(dto.getSyn())
                .intVal(dto.getIntVal())
                .build());
        messagingTemplate.convertAndSend("/topic/input/perte", dto);
        log.info("Perte broadcasted on /topic/input/perte for date={}", perte.getDate());
        return recalculerDepuisDate(perte.getDate());
    }

    @Transactional
    public IndicateursDTO ingestGypse(AnalyseGypseDTO dto) {
        AnalyseGypse gypse = sauvegarderGypse(dto);
        messagingTemplate.convertAndSend("/topic/input/gypse", dto);
        log.info("Gypse broadcasted on /topic/input/gypse for date={}", gypse.getDate());
        return recalculerDepuisDate(gypse.getDate());
    }

    @Transactional
    public IndicateursDTO ingestPhosphate(AnalysePhosphateDTO dto) {
        AnalysePhosphate phosphate = sauvegarderPhosphate(dto);
        return recalculerDepuisDate(phosphate.getDate());
    }

    @Transactional
    public IndicateursDTO ingestProduction(ProductionDTO dto) {
        Production production = sauvegarderProduction(dto);
        return recalculerDepuisDate(production.getDate());
    }

    @Transactional
    public IndicateursDTO ingestConsommation(ConsommationDTO dto) {
        Consommation conso = sauvegarderConsommation(dto);
        return recalculerDepuisDate(conso.getDate());
    }

    // ── LECTURE ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<IndicateursDTO> getDernierIndicateur() {
        return indicateursRepo.findTopByOrderByDateDesc().map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<IndicateursDTO> getIndicateursSurPeriode(LocalDateTime debut, LocalDateTime fin) {
        return indicateursRepo.findByDateBetweenOrderByDateAsc(debut, fin)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IndicateursDTO> getHistoriqueRecent() {
        return indicateursRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AnalyseGypseDTO> getDernierGypse() {
        return gypseRepo.findTopByOrderByDateDesc().map(this::toGypseDTO);
    }

    @Transactional(readOnly = true)
    public List<AnalyseGypseDTO> getHistoriqueGypse() {
        return gypseRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toGypseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PerteDTO> getDernierPerte() {
        return perteRepo.findTopByOrderByDateDesc().map(this::toPerteDTO);
    }

    @Transactional(readOnly = true)
    public List<PerteDTO> getHistoriquePerte() {
        return perteRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toPerteDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AnalysePhosphateDTO> getDernierPhosphate() {
        return phosphateRepo.findTopByOrderByDateDesc().map(this::toPhosphateDTO);
    }

    @Transactional(readOnly = true)
    public List<AnalysePhosphateDTO> getHistoriquePhosphate() {
        return phosphateRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toPhosphateDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductionDTO> getDerniereProduction() {
        return productionRepo.findTopByOrderByDateDesc().map(this::toProductionDTO);
    }

    @Transactional(readOnly = true)
    public List<ProductionDTO> getHistoriqueProduction() {
        return productionRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toProductionDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PerteDTO> getPertesSurPeriode(LocalDateTime debut, LocalDateTime fin) {
        return perteRepo.findByDateBetweenOrderByDateAsc(debut, fin)
                .stream().map(this::toPerteDTO).collect(Collectors.toList());
    }

    // ── MOTEUR DE CALCUL ──────────────────────────────────────────

    private IndicateursDTO recalculerDepuisDate(LocalDateTime dateRef) {
        LocalDateTime debut = dateRef.minusMinutes(TOLERANCE_MINUTES);
        LocalDateTime fin   = dateRef.plusMinutes(TOLERANCE_MINUTES);

        Optional<AnalyseGypse>     gypse      = gypseRepo.findClosestToDate(dateRef, debut, fin);
        Optional<AnalysePhosphate> phosphate  = phosphateRepo.findClosestToDate(dateRef, debut, fin);
        Optional<Production>       production = productionRepo.findClosestToDate(dateRef, debut, fin);
        Optional<Consommation>     conso      = consommationRepo.findClosestToDate(dateRef, debut, fin);
        Optional<Perte>            perte      = perteRepo.findClosestToDate(dateRef, debut, fin);

        if (gypse.isEmpty() || phosphate.isEmpty() || production.isEmpty()
                || conso.isEmpty() || perte.isEmpty()) {
            log.warn("Données incomplètes pour date={} — indicateurs non calculés", dateRef);
            return null;
        }

        IndicateursCalcules indicateurs = calculer(
                dateRef, gypse.get(), phosphate.get(), production.get(), conso.get());

        IndicateursCalcules saved = indicateursRepo.save(indicateurs);
        log.info("Indicateurs calculés et sauvegardés pour date={}", dateRef);

        IndicateursDTO result = toDTO(saved);
        messagingTemplate.convertAndSend("/topic/indicateurs", result);

        // Pass perte to alert service so SE/SYN/INT alerts can fire
        alerteService.verifierEtCreerAlertes(saved, perte.get());
        predictionService.notifyNewData();

        return result;
    }

    private IndicateursCalcules calculer(
            LocalDateTime dateRef,
            AnalyseGypse gypse,
            AnalysePhosphate phosphate,
            Production production,
            Consommation conso) {

        log.info("CALCULATION START for {}", dateRef);

        Double p2o5Gypse = moyenne(gypse.getP2o5GypseA(), gypse.getP2o5GypseB());
        Double caOGypse  = moyenne(gypse.getCaOGypseA(),  gypse.getCaOGypseB());

        Double p2o5Phosphate = phosphate.getP2o5Phosphate();
        Double caOPhosphate  = phosphate.getCaOPhosphate();
        Double qPhosphate    = phosphate.getQPhosphate();

        Double q29    = production.getQP2o529();
        Double q54    = production.getQP2o554();
        Double h2so4  = conso.getQteH2so4();
        Double eau    = conso.getQteEauBrute();
        Double phosph = conso.getQtePhosphates();
        Double vapeur = conso.getQteVapeur();

        Double rc = null;
        if (p2o5Gypse != null && caOPhosphate != null && p2o5Phosphate != null
                && caOGypse != null && p2o5Phosphate != 0 && caOGypse != 0) {
            rc = 1.0 - ((p2o5Gypse * caOPhosphate) / (p2o5Phosphate * caOGypse));
        }

        Double ri = null;
        if (q29 != null && p2o5Phosphate != null && qPhosphate != null) {
            Double denom = (p2o5Phosphate * qPhosphate) / 100.0;
            if (denom != 0) ri = q29 / denom;
        }

        Double cap           = (q29 != null && q54 != null && q29 > 0.1) ? q54 / q29 : null;
        Double consoH2so4    = (h2so4  != null && q29 != null && q29 != 0) ? h2so4  / q29 : null;
        Double consoEauBrute = (eau    != null && q29 != null && q29 != 0) ? eau    / q29 : null;
        Double consoPhos     = (phosph != null && q29 != null && q29 != 0) ? phosph / q29 : null;
        Double consoVapeur   = (vapeur != null && q54 != null && q54 != 0) ? vapeur / q54 : null;

        return IndicateursCalcules.builder()
                .date(dateRef)
                .rc(arrondir(rc))
                .ri(arrondir(ri))
                .cap(arrondir(cap))
                .consoH2so4(arrondir(consoH2so4))
                .consoEauBrute(arrondir(consoEauBrute))
                .consoPhosphates(arrondir(consoPhos))
                .consoVapeur(arrondir(consoVapeur))
                .build();
    }

    // ── MAPPERS ───────────────────────────────────────────────────

    private AnalyseGypse sauvegarderGypse(AnalyseGypseDTO dto) {
        return gypseRepo.save(AnalyseGypse.builder()
                .date(dto.getDate())
                .p2o5GypseA(dto.getP2o5GypseA()).p2o5GypseB(dto.getP2o5GypseB())
                .caOGypseA(dto.getCaOGypseA()).caOGypseB(dto.getCaOGypseB())
                .build());
    }

    private AnalysePhosphate sauvegarderPhosphate(AnalysePhosphateDTO dto) {
        return phosphateRepo.save(AnalysePhosphate.builder()
                .date(dto.getDate())
                .p2o5Phosphate(dto.getP2o5Phosphate())
                .caOPhosphate(dto.getCaOPhosphate())
                .qPhosphate(dto.getQPhosphate())
                .build());
    }

    private Production sauvegarderProduction(ProductionDTO dto) {
        return productionRepo.save(Production.builder()
                .date(dto.getDate())
                .qP2o529(dto.getQP2o529())
                .qP2o554(dto.getQP2o554())
                .build());
    }

    private Consommation sauvegarderConsommation(ConsommationDTO dto) {
        return consommationRepo.save(Consommation.builder()
                .date(dto.getDate())
                .qteH2so4(dto.getQteH2so4())
                .qteEauBrute(dto.getQteEauBrute())
                .qtePhosphates(dto.getQtePhosphates())
                .qteVapeur(dto.getQteVapeur())
                .build());
    }

    private IndicateursDTO toDTO(IndicateursCalcules e) {
        IndicateursDTO dto = new IndicateursDTO();
        dto.setId(e.getId());
        dto.setDate(e.getDate());
        dto.setRc(e.getRc());
        dto.setRi(e.getRi());
        dto.setCap(e.getCap());
        dto.setConsoH2so4(e.getConsoH2so4());
        dto.setConsoEauBrute(e.getConsoEauBrute());
        dto.setConsoPhosphates(e.getConsoPhosphates());
        dto.setConsoVapeur(e.getConsoVapeur());
        return dto;
    }

    private AnalyseGypseDTO toGypseDTO(AnalyseGypse e) {
        AnalyseGypseDTO dto = new AnalyseGypseDTO();
        dto.setDate(e.getDate());
        dto.setP2o5GypseA(e.getP2o5GypseA());
        dto.setP2o5GypseB(e.getP2o5GypseB());
        dto.setCaOGypseA(e.getCaOGypseA());
        dto.setCaOGypseB(e.getCaOGypseB());
        return dto;
    }

    private PerteDTO toPerteDTO(Perte e) {
        return PerteDTO.builder()
                .date(e.getDate())
                .se(e.getSe())
                .syn(e.getSyn())
                .intVal(e.getIntVal())
                .build();
    }

    private AnalysePhosphateDTO toPhosphateDTO(AnalysePhosphate e) {
        AnalysePhosphateDTO dto = new AnalysePhosphateDTO();
        dto.setDate(e.getDate());
        dto.setP2o5Phosphate(e.getP2o5Phosphate());
        dto.setCaOPhosphate(e.getCaOPhosphate());
        dto.setQPhosphate(e.getQPhosphate());
        return dto;
    }

    private ProductionDTO toProductionDTO(Production e) {
        ProductionDTO dto = new ProductionDTO();
        dto.setDate(e.getDate());
        dto.setQP2o529(e.getQP2o529());
        dto.setQP2o554(e.getQP2o554());
        return dto;
    }

    private Double moyenne(Double a, Double b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return (a + b) / 2.0;
    }

    private Double arrondir(Double v) {
        if (v == null) return null;
        return Math.round(v * 10000.0) / 10000.0;
    }
}