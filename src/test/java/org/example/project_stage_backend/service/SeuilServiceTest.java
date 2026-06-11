package org.example.project_stage_backend.service;

import org.example.project_stage_backend.dto.SeuilDTO;
import org.example.project_stage_backend.entity.Seuil;
import org.example.project_stage_backend.repository.SeuilRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeuilServiceTest {

    private final SeuilRepository seuilRepository = mock(SeuilRepository.class);
    private final SeuilService seuilService = new SeuilService(seuilRepository);

    @Test
    void niveauReturnsWarningAndCriticalForMaximumThresholds() {
        Seuil seuil = seuil(Seuil.Type.MAX, 10.0, 15.0);

        assertThat(seuilService.niveau(seuil, 9.9)).isNull();
        assertThat(seuilService.niveau(seuil, 10.0)).isEqualTo(SeuilService.AlerteNiveau.WARNING);
        assertThat(seuilService.niveau(seuil, 15.0)).isEqualTo(SeuilService.AlerteNiveau.CRITICAL);
    }

    @Test
    void niveauReturnsWarningAndCriticalForMinimumThresholds() {
        Seuil seuil = seuil(Seuil.Type.MIN, 90.0, 80.0);

        assertThat(seuilService.niveau(seuil, 91.0)).isNull();
        assertThat(seuilService.niveau(seuil, 90.0)).isEqualTo(SeuilService.AlerteNiveau.WARNING);
        assertThat(seuilService.niveau(seuil, 80.0)).isEqualTo(SeuilService.AlerteNiveau.CRITICAL);
    }

    @Test
    void updatePersistsValidThresholds() {
        Seuil seuil = seuil(Seuil.Type.MAX, 10.0, 15.0);
        seuil.setCode("cap");
        seuil.setLabel("CAP");

        when(seuilRepository.findByCode("cap")).thenReturn(Optional.of(seuil));
        when(seuilRepository.save(any(Seuil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeuilDTO dto = seuilService.update("cap", new SeuilDTO.UpdateRequest(12.0, 18.0));

        assertThat(dto.getWarning()).isEqualTo(12.0);
        assertThat(dto.getCritique()).isEqualTo(18.0);
        verify(seuilRepository).save(seuil);
    }

    @Test
    void updateRejectsMaximumWarningAboveCriticalThreshold() {
        when(seuilRepository.findByCode("cap")).thenReturn(Optional.of(seuil(Seuil.Type.MAX, 10.0, 15.0)));

        assertThatThrownBy(() -> seuilService.update("cap", new SeuilDTO.UpdateRequest(20.0, 18.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("warning doit etre inferieur");
    }

    @Test
    void isEnAlerteReturnsFalseForMissingValuesAndUnknownCodes() {
        when(seuilRepository.findByCode("missing")).thenReturn(Optional.empty());

        assertThat(seuilService.isEnAlerte("cap", null)).isFalse();
        assertThat(seuilService.isEnAlerte("missing", 12.0)).isFalse();
    }

    private Seuil seuil(Seuil.Type type, Double warning, Double critique) {
        return Seuil.builder()
                .type(type)
                .warning(warning)
                .critique(critique)
                .build();
    }
}
