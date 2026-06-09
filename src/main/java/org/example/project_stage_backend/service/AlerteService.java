package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project_stage_backend.dto.AlerteDTO;
import org.example.project_stage_backend.entity.Alerte;
import org.example.project_stage_backend.entity.Alerte.Severite;
import org.example.project_stage_backend.entity.IndicateursCalcules;
import org.example.project_stage_backend.entity.Perte;
import org.example.project_stage_backend.entity.Seuil;
import org.example.project_stage_backend.repository.AlerteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlerteService {

    private final AlerteRepository alerteRepo;
    private final SeuilService seuilService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final Map<String, LocalDateTime> dernierEnvoiN8n = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MINUTES = 30L;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    public void verifierEtCreerAlertes(IndicateursCalcules ind, Perte perte) {
        List<Alerte> alertes = new ArrayList<>();

        if (perte != null) {
            verifier(alertes, "se",     "SE",  perte.getSe(),     ind.getDate());
            verifier(alertes, "syn",    "SYN", perte.getSyn(),    ind.getDate());
            verifier(alertes, "intVal", "INT", perte.getIntVal(), ind.getDate());
        }

        verifier(alertes, "consoH2so4",      "CONSO_H2SO4",      ind.getConsoH2so4(),      ind.getDate());
        verifier(alertes, "consoEauBrute",   "CONSO_EAU_BRUTE",  ind.getConsoEauBrute(),   ind.getDate());
        verifier(alertes, "consoPhosphates", "CONSO_PHOSPHATES", ind.getConsoPhosphates(), ind.getDate());
        verifier(alertes, "consoVapeur",     "CONSO_VAPEUR",     ind.getConsoVapeur(),     ind.getDate());

        verifier(alertes, "rc", "RC", ind.getRc(), ind.getDate());
        verifier(alertes, "ri", "RI", ind.getRi(), ind.getDate());

        if (alertes.isEmpty()) return;

        List<Alerte> saved = alerteRepo.saveAll(alertes);
        log.info("{} alerte(s) creee(s) pour date={}", saved.size(), ind.getDate());

        List<AlerteDTO> dtos = saved.stream().map(AlerteDTO::from).collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/alertes", dtos);

        saved.stream()
                .filter(a -> a.getSeverite() == Severite.CRITICAL)
                .forEach(this::notifierN8n);
    }

    private void verifier(List<Alerte> alertes, String codeSeuil, String typeIndicateur, Double valeur, LocalDateTime date) {
        if (valeur == null) return;

        Optional<Seuil> seuil = seuilService.findByCode(codeSeuil);
        if (seuil.isEmpty()) return;

        SeuilService.AlerteNiveau niveau = seuilService.niveau(seuil.get(), valeur);
        if (niveau == null) return;

        Severite severite = niveau == SeuilService.AlerteNiveau.CRITICAL
                ? Severite.CRITICAL
                : Severite.WARNING;
        double seuilRef = niveau == SeuilService.AlerteNiveau.CRITICAL
                ? seuil.get().getCritique()
                : seuil.get().getWarning();

        alertes.add(creerAlerte(date, typeIndicateur, valeur, seuilRef, severite));
    }

    private Alerte creerAlerte(LocalDateTime date, String type,
                               Double valeur, double seuil, Severite sev) {
        return Alerte.builder()
                .date(date)
                .typeIndicateur(type)
                .valeur(valeur)
                .seuil(seuil)
                .severite(sev)
                .acquittee(false)
                .build();
    }

    private void notifierN8n(Alerte alerte) {
        String key = alerte.getTypeIndicateur();
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dernierEnvoi = dernierEnvoiN8n.get(key);

        if (dernierEnvoi != null &&
                dernierEnvoi.plusMinutes(COOLDOWN_MINUTES).isAfter(maintenant)) {
            log.info("Cooldown actif pour {} - email ignore", key);
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "indicateur", alerte.getTypeIndicateur(),
                    "valeur",     alerte.getValeur(),
                    "seuil",      alerte.getSeuil(),
                    "severite",   alerte.getSeverite().name(),
                    "date",       alerte.getDate().toString(),
                    "unite",      "JFC1 - OCP"
            );
            restTemplate.postForEntity(n8nWebhookUrl, payload, String.class);
            dernierEnvoiN8n.put(key, maintenant);
            log.info("n8n notifie pour {} - prochain dans {} min", key, COOLDOWN_MINUTES);
        } catch (Exception e) {
            log.error("Echec notification n8n : {}", e.getMessage());
        }
    }

    public List<AlerteDTO> getAlertesNonAcquittees() {
        return alerteRepo.findTop50ByAcquitteeOrderByDateDesc(false)
                .stream().map(AlerteDTO::from).collect(Collectors.toList());
    }

    public List<AlerteDTO> getHistoriqueAlertes() {
        return alerteRepo.findTop100ByOrderByDateDesc()
                .stream().map(AlerteDTO::from).collect(Collectors.toList());
    }

    public AlerteDTO acquitter(Long id) {
        Alerte alerte = alerteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerte introuvable : " + id));
        alerte.setAcquittee(true);
        return AlerteDTO.from(alerteRepo.save(alerte));
    }
}
