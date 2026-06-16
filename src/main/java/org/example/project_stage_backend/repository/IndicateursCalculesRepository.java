package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.IndicateursCalcules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicateursCalculesRepository extends JpaRepository<IndicateursCalcules, Long> {

    Optional<IndicateursCalcules> findByDate(LocalDateTime date);

    List<IndicateursCalcules> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);

    Optional<IndicateursCalcules> findTopByOrderByDateDesc();

    // Pour l'historique paginé du frontend
    List<IndicateursCalcules> findTop100ByOrderByDateDesc();
}
