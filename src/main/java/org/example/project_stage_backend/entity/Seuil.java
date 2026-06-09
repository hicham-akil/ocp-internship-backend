package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seuils", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seuil_code", columnNames = "code")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seuil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Type type;

    @Column
    private Double warning;

    @Column(nullable = false)
    private Double critique;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public enum Type {
        MAX,
        MIN
    }
}
