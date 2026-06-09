package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.SeuilDTO;
import org.example.project_stage_backend.entity.Seuil;
import org.example.project_stage_backend.repository.SeuilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeuilService {

    private final SeuilRepository seuilRepository;

    private static final List<DefaultSeuil> DEFAULTS = List.of(
            new DefaultSeuil("se",              "Perte SE",                 Seuil.Type.MAX, 0.60, 0.90),
            new DefaultSeuil("syn",             "Perte SYN",                Seuil.Type.MAX, 0.78, 0.86),
            new DefaultSeuil("intVal",          "Perte INT",                Seuil.Type.MAX, 0.21, 0.23),
            new DefaultSeuil("rc",              "Rendement chimique RC",    Seuil.Type.MIN, 0.93, 0.90),
            new DefaultSeuil("ri",              "Rendement industriel RI",  Seuil.Type.MIN, null, 0.80),
            new DefaultSeuil("consoH2so4",      "Consommation H2SO4",       Seuil.Type.MAX, null, 3.8),
            new DefaultSeuil("consoEauBrute",   "Consommation eau brute",   Seuil.Type.MAX, null, 15.0),
            new DefaultSeuil("consoPhosphates", "Consommation phosphates",  Seuil.Type.MAX, 3.5, 4.2),
            new DefaultSeuil("consoVapeur",     "Consommation vapeur",      Seuil.Type.MAX, 1.2, 1.5),
            new DefaultSeuil("cap",             "Capacite CAP",            Seuil.Type.MAX, null, 2.0),
            new DefaultSeuil("p2o5Gypse",       "P2O5 gypse",               Seuil.Type.MAX, null, 3.5),
            new DefaultSeuil("caOGypse",        "CaO gypse",                Seuil.Type.MAX, null, 33.0),
            new DefaultSeuil("p2o5Phosphate",   "P2O5 phosphate",           Seuil.Type.MIN, null, 27.0),
            new DefaultSeuil("caOPhosphate",    "CaO phosphate",            Seuil.Type.MIN, null, 39.0)
    );

    @Transactional
    public void initialiserDefaults() {
        DEFAULTS.forEach(def -> {
            if (!seuilRepository.existsByCode(def.code())) {
                seuilRepository.save(Seuil.builder()
                        .code(def.code())
                        .label(def.label())
                        .type(def.type())
                        .warning(def.warning())
                        .critique(def.critique())
                        .build());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<SeuilDTO> getAll() {
        return seuilRepository.findAll().stream()
                .sorted(Comparator.comparing(Seuil::getId))
                .map(SeuilDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Seuil> findByCode(String code) {
        return seuilRepository.findByCode(code);
    }

    @Transactional
    public SeuilDTO update(String code, SeuilDTO.UpdateRequest request) {
        Seuil seuil = seuilRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Seuil introuvable : " + code));

        validate(seuil.getType(), request.getWarning(), request.getCritique());

        seuil.setWarning(request.getWarning());
        seuil.setCritique(request.getCritique());
        return SeuilDTO.from(seuilRepository.save(seuil));
    }

    @Transactional(readOnly = true)
    public boolean isEnAlerte(String code, Double valeur) {
        if (valeur == null) return false;
        return seuilRepository.findByCode(code)
                .map(seuil -> niveau(seuil, valeur) != null)
                .orElse(false);
    }

    public AlerteNiveau niveau(Seuil seuil, Double valeur) {
        if (valeur == null || seuil == null || seuil.getCritique() == null) return null;

        if (seuil.getType() == Seuil.Type.MAX) {
            if (valeur >= seuil.getCritique()) return AlerteNiveau.CRITICAL;
            if (seuil.getWarning() != null && valeur >= seuil.getWarning()) return AlerteNiveau.WARNING;
            return null;
        }

        if (valeur <= seuil.getCritique()) return AlerteNiveau.CRITICAL;
        if (seuil.getWarning() != null && valeur <= seuil.getWarning()) return AlerteNiveau.WARNING;
        return null;
    }

    private void validate(Seuil.Type type, Double warning, Double critique) {
        if (critique == null) {
            throw new IllegalArgumentException("Le seuil critique est obligatoire.");
        }

        if (warning == null) return;

        if (type == Seuil.Type.MAX && warning > critique) {
            throw new IllegalArgumentException("Pour un seuil maximum, warning doit etre inferieur ou egal au critique.");
        }

        if (type == Seuil.Type.MIN && warning < critique) {
            throw new IllegalArgumentException("Pour un seuil minimum, warning doit etre superieur ou egal au critique.");
        }
    }

    public enum AlerteNiveau {
        WARNING,
        CRITICAL
    }

    private record DefaultSeuil(String code, String label, Seuil.Type type, Double warning, Double critique) {}
}
