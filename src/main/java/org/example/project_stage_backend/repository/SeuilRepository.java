package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.Seuil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeuilRepository extends JpaRepository<Seuil, Long> {
    Optional<Seuil> findByCode(String code);
    boolean existsByCode(String code);
}
