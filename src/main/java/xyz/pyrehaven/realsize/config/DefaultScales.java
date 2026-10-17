package xyz.pyrehaven.realsize.config;

import java.util.Map;

public final class DefaultScales {
    private DefaultScales() {
    }

    public static RealSizeConfig config() {
        // Retain the v1 table; bounds are applied by SizingPolicy, not baked into rules.
        // Registry membership and living-entity eligibility are checked in GameTests.
        return new RealSizeConfig(2, 0.22, 0.22, 1.45, true, 1.49, true, Map.ofEntries(
                Map.entry("minecraft:spider", 0.26),
                Map.entry("minecraft:cave_spider", 0.20),
                Map.entry("minecraft:silverfish", 0.22),
                Map.entry("minecraft:endermite", 0.22),
                Map.entry("minecraft:bee", 0.25),
                Map.entry("minecraft:bat", 0.24),
                Map.entry("minecraft:frog", 0.28),
                Map.entry("minecraft:tadpole", 0.22),
                Map.entry("minecraft:axolotl", 0.55),
                Map.entry("minecraft:tropical_fish", 0.22),
                Map.entry("minecraft:pufferfish", 0.43),
                Map.entry("minecraft:cod", 0.5),
                Map.entry("minecraft:salmon", 0.7),
                Map.entry("minecraft:squid", 0.4),
                Map.entry("minecraft:glow_squid", 0.4),
                Map.entry("minecraft:nautilus", 0.4),
                Map.entry("minecraft:zombie_nautilus", 0.4),
                Map.entry("minecraft:turtle", 0.55),
                Map.entry("minecraft:chicken", 0.64),
                Map.entry("minecraft:parrot", 0.45),
                Map.entry("minecraft:rabbit", 0.5),
                Map.entry("minecraft:cat", 0.45),
                Map.entry("minecraft:fox", 0.5),
                Map.entry("minecraft:armadillo", 0.38),
                Map.entry("minecraft:allay", 0.42),
                Map.entry("minecraft:vex", 0.3),
                Map.entry("minecraft:ocelot", 0.64),
                Map.entry("minecraft:goat", 0.65),
                Map.entry("minecraft:wolf", 0.88),
                Map.entry("minecraft:pig", 0.95),
                Map.entry("minecraft:sheep", 0.75),
                Map.entry("minecraft:panda", 0.92),
                Map.entry("minecraft:polar_bear", 1.05),
                Map.entry("minecraft:hoglin", 0.85),
                Map.entry("minecraft:zoglin", 0.85),
                Map.entry("minecraft:donkey", 0.88),
                Map.entry("minecraft:mule", 0.93),
                Map.entry("minecraft:horse", 1.05),
                Map.entry("minecraft:skeleton_horse", 1.05),
                Map.entry("minecraft:zombie_horse", 1.05),
                Map.entry("minecraft:llama", 0.92),
                Map.entry("minecraft:trader_llama", 0.92),
                Map.entry("minecraft:camel", 1.1),
                Map.entry("minecraft:sniffer", 0.94),
                Map.entry("minecraft:dolphin", 1.3),
                Map.entry("minecraft:guardian", 0.7),
                Map.entry("minecraft:elder_guardian", 1.35),
                Map.entry("minecraft:piglin_brute", 1.05),
                Map.entry("minecraft:iron_golem", 1.2),
                Map.entry("minecraft:ravager", 1.15)
        ));
    }
}
