package xyz.pyrehaven.realsize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RealSizeConfig(
        double floor,
        double cap,
        double trackingRangeThreshold,
        int minTrackingRangeChunks,
        int minTrackingDistanceBlocks,
        double stepHeightBoostThreshold,
        double stepHeightBoostAmount,
        Map<String, Double> entityScales
) {
    public RealSizeConfig {
        entityScales = Collections.unmodifiableMap(new LinkedHashMap<>(entityScales));
    }

    public static RealSizeConfig defaults() {
        Map<String, Double> entityScales = new LinkedHashMap<>();

        entityScales.put("minecraft:spider", 0.26);
        entityScales.put("minecraft:cave_spider", 0.20);
        entityScales.put("minecraft:silverfish", 0.22);
        entityScales.put("minecraft:endermite", 0.22);
        entityScales.put("minecraft:bee", 0.25);
        entityScales.put("minecraft:bat", 0.24);
        entityScales.put("minecraft:frog", 0.28);
        entityScales.put("minecraft:tadpole", 0.22);
        entityScales.put("minecraft:axolotl", 0.55);
        entityScales.put("minecraft:tropical_fish", 0.22);
        entityScales.put("minecraft:pufferfish", 0.43);
        entityScales.put("minecraft:cod", 0.50);
        entityScales.put("minecraft:salmon", 0.70);
        entityScales.put("minecraft:squid", 0.40);
        entityScales.put("minecraft:glow_squid", 0.40);
        entityScales.put("minecraft:nautilus", 0.40);
        entityScales.put("minecraft:zombie_nautilus", 0.40);
        entityScales.put("minecraft:turtle", 0.55);
        entityScales.put("minecraft:chicken", 0.64);
        entityScales.put("minecraft:parrot", 0.45);
        entityScales.put("minecraft:rabbit", 0.50);
        entityScales.put("minecraft:cat", 0.45);
        entityScales.put("minecraft:fox", 0.50);
        entityScales.put("minecraft:armadillo", 0.38);
        entityScales.put("minecraft:allay", 0.42);
        entityScales.put("minecraft:vex", 0.30);
        entityScales.put("minecraft:ocelot", 0.64);
        entityScales.put("minecraft:goat", 0.65);
        entityScales.put("minecraft:wolf", 0.88);
        entityScales.put("minecraft:pig", 0.95);
        entityScales.put("minecraft:sheep", 0.75);
        entityScales.put("minecraft:panda", 0.92);
        entityScales.put("minecraft:polar_bear", 1.05);
        entityScales.put("minecraft:hoglin", 0.85);
        entityScales.put("minecraft:zoglin", 0.85);
        entityScales.put("minecraft:donkey", 0.88);
        entityScales.put("minecraft:mule", 0.93);
        entityScales.put("minecraft:horse", 1.05);
        entityScales.put("minecraft:skeleton_horse", 1.05);
        entityScales.put("minecraft:zombie_horse", 1.05);
        entityScales.put("minecraft:llama", 0.92);
        entityScales.put("minecraft:trader_llama", 0.92);
        entityScales.put("minecraft:camel", 1.10);
        entityScales.put("minecraft:sniffer", 0.94);
        entityScales.put("minecraft:dolphin", 1.30);
        entityScales.put("minecraft:guardian", 0.70);
        entityScales.put("minecraft:elder_guardian", 1.35);
        entityScales.put("minecraft:piglin_brute", 1.05);
        entityScales.put("minecraft:iron_golem", 1.20);
        entityScales.put("minecraft:ravager", 1.15);

        return new RealSizeConfig(
                0.22,
                1.45,
                0.60,
                10,
                128,
                1.10,
                0.5,
                entityScales
        );
    }

    public double scaleFor(String entityId, double fallbackScale) {
        return entityScales.getOrDefault(entityId, fallbackScale);
    }
}
