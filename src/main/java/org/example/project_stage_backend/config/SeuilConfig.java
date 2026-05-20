package org.example.project_stage_backend.config;

import org.springframework.stereotype.Component;
import java.util.Map;


@Component
public class SeuilConfig {

    // Indicators where HIGH value is bad
    public static final Map<String, double[]> SEUILS_MAX = Map.of(
            "SE",               new double[]{0.18, 0.20},
            "SYN",              new double[]{0.65, 0.67},
            "INT",              new double[]{0.20, 0.22},
            "CONSO_H2SO4",      new double[]{3.2,  3.8},
            "CONSO_EAU_BRUTE",  new double[]{15.0, 18.0},
            "CONSO_PHOSPHATES", new double[]{3.5,  4.2},
            "CONSO_VAPEUR",     new double[]{1.2,  1.5}
    );

    // Indicators where LOW value is bad
    public static final Map<String, double[]> SEUILS_MIN = Map.of(
            "RC", new double[]{0.95, 0.94},
            "RI", new double[]{0.94, 0.93}
    );

    // Convenience: returns the WARNING threshold (index 0) for display in reports
    public static double warningMax(String key) {
        return SEUILS_MAX.get(key)[0];
    }

    public static double warningMin(String key) {
        return SEUILS_MIN.get(key)[0];
    }
}