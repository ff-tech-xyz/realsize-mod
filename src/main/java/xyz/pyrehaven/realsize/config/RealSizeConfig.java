package xyz.pyrehaven.realsize.config;

import java.util.Map;

public record RealSizeConfig(
        int configVersion,
        double floor,
        double hostileFloor,
        double cap,
        boolean proportionalStepHeight,
        double maxStepHeight,
        boolean clientRenderDistanceFix,
        Map<String, Double> entityScales
) {
    public RealSizeConfig {
        if (configVersion != 2) {
            throw new IllegalArgumentException("configVersion: expected 2");
        }
        positive("floor", floor);
        positive("hostileFloor", hostileFloor);
        positive("cap", cap);
        positive("maxStepHeight", maxStepHeight);
        if (hostileFloor < floor) {
            throw new IllegalArgumentException("hostileFloor: must be at least floor");
        }
        if (cap < hostileFloor || cap < floor) {
            throw new IllegalArgumentException("cap: must be at least floor and hostileFloor");
        }
        if (maxStepHeight >= 1.5) {
            throw new IllegalArgumentException("maxStepHeight: must be below 1.5");
        }
        entityScales.forEach((selector, scale) -> {
            if (!selector.matches("#?[a-z0-9_.-]+:[a-z0-9/._-]+")) {
                throw new IllegalArgumentException("entityScales[" + selector + "]: invalid ID/tag syntax");
            }
            positive("entityScales[" + selector + "]", scale);
        });
        entityScales = Map.copyOf(entityScales);
    }

    private static void positive(String field, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(field + ": must be positive and finite");
        }
    }
}
