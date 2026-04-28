package xyz.pyrehaven.realsize;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * RealSize v1.0.9 — Accurate real-world scaling, all 1.21.11 mobs.
 * Small-mob culling fix via Mixin on EntityType.getTrackingRange() —
 * no reflection, no fragile field discovery.
 *
 * Reference: Player = 1.95m = 1.95 blocks (call it 2.0)
 * Scale = real_height_m / vanilla_height_blocks
 * Hard floor: 0.22 (spider/bee level — anything smaller is invisible)
 * Hard cap:   1.45 (prevents extreme ceiling clipping)
 *
 * Real-world heights used (shoulder/body height, not nose-to-tail):
 *   Bee:            0.015m   Vanilla: 0.60m  → 0.025 → floor 0.22
 *   Silverfish:     0.015m   Vanilla: 0.30m  → 0.05  → floor 0.22
 *   Cave Spider:    0.02m    Vanilla: 0.50m  → 0.04  → floor 0.22
 *   Spider:         0.025m   Vanilla: 0.90m  → 0.03  → floor 0.22
 *   Bat:            0.06m    Vanilla: 0.90m  → 0.07  → floor 0.22
 *   Endermite:      0.02m    Vanilla: 0.30m  → 0.07  → floor 0.22
 *   Tadpole:        0.015m   Vanilla: 0.40m  → 0.04  → floor 0.22
 *   Tropical Fish:  0.05m    Vanilla: 0.60m  → 0.08  → floor 0.22
 *   Frog:           0.10m    Vanilla: 0.55m  → 0.18  → 0.28 (bump for visibility)
 *   Axolotl:        0.25m    Vanilla: 0.42m  → 0.60  → 0.55 (scale down to match)
 *   Rabbit:         0.25m    Vanilla: 0.50m  → 0.50
 *   Pufferfish:     0.30m    Vanilla: 0.70m  → 0.43  → 0.43
 *   Parrot:         0.35m    Vanilla: 0.90m  → 0.39  → 0.45 (bump slightly)
 *   Cat:            0.25m    Vanilla: 0.70m  → 0.36  → 0.45
 *   Turtle:         0.45m    Vanilla: 0.40m  → 1.13  → 0.55 (vanilla already small)
 *   Cod:            0.50m    Vanilla: 0.30m  → 1.67  → cap — 0.50 feels right
 *   Squid:          0.30m    Vanilla: 0.80m  → 0.38  → 0.40
 *   Salmon:         0.60m    Vanilla: 0.50m  → 1.20  → 0.70 (vanilla already reasonable)
 *   Fox:            0.35m    Vanilla: 0.70m  → 0.50
 *   Ocelot:         0.45m    Vanilla: 0.70m  → 0.64
 *   Armadillo:      0.20m    Vanilla: 0.60m  → 0.33  → 0.38
 *   Allay:          0.25m    Vanilla: 0.60m  → 0.42  → 0.42
 *   Vex:            0.20m    Vanilla: 0.80m  → 0.25  → 0.30
 *   Chicken:        0.45m    Vanilla: 0.70m  → 0.64
 *   Guardians:      0.90m    Vanilla: 0.85m  → 1.06  → 0.70
 *   Goat:           0.75m    Vanilla: 1.30m  → 0.58  → 0.65
 *   Wolf:           0.80m    Vanilla: 0.85m  → 0.94  → 0.88
 *   Pig:            0.90m    Vanilla: 0.90m  → 1.00  → 0.95 (minor tweak)
 *   Sheep:          0.90m    Vanilla: 1.30m  → 0.69  → 0.75
 *   Panda:          1.20m    Vanilla: 1.30m  → 0.92
 *   Polar Bear:     1.50m    Vanilla: 1.40m  → 1.07  → 1.05
 *   Hoglin/Zoglin:  1.10m    Vanilla: 1.40m  → 0.79  → 0.85
 *   Donkey:         1.20m    Vanilla: 1.40m  → 0.86  → 0.88
 *   Mule:           1.30m    Vanilla: 1.40m  → 0.93
 *   Horse:          1.60m    Vanilla: 1.60m  → 1.00  → 1.05 (slight bump)
 *   Llama:          1.80m    Vanilla: 1.95m  → 0.92
 *   Sniffer:        1.70m    Vanilla: 1.80m  → 0.94
 *   Camel:          2.00m    Vanilla: 1.87m  → 1.07  → 1.10
 *   Dolphin:        2.00m    Vanilla: 0.60m  → 3.33  → cap 1.30
 *   Ravager:        2.00m    Vanilla: 1.90m  → 1.05  → 1.15
 *   Elder Guardian: 2.00m    Vanilla: 1.99m  → 1.00  → 1.35 (should feel huge)
 *   Iron Golem:     2.70m    Vanilla: 2.70m  → 1.00  → 1.20 (bump for impact)
 *   Piglin Brute:   2.00m    Vanilla: 1.95m  → 1.03  → 1.05
 *
 * Stat adjustments:
 *   scale > 1.10 → STEP_HEIGHT boost: ADD_VALUE +0.5
 *   scale < TRACKING_RANGE_THRESHOLD → EntityType.getTrackingRange() boosted via Mixin
 */
public class RealSizeMod implements ModInitializer {
    public static final String MOD_ID = "realsize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Identifier ID_SCALE       = Identifier.fromNamespaceAndPath(MOD_ID, "scale");
    private static final Identifier ID_STEP_HEIGHT = Identifier.fromNamespaceAndPath(MOD_ID, "step_height");

    public static final double FLOOR = 0.22;
    public static final double CAP   = 1.45;

    /**
     * Mobs scaled below this threshold get their EntityType tracking range boosted
     * via the EntityTypeTrackingMixin so they are never culled at normal view distances.
     */
    public static final double TRACKING_RANGE_THRESHOLD = 0.60;

    /**
     * Minimum tracking range in CHUNKS for EntityType.getMaxTrackDistance() mixin.
     * This feeds into EntityTracker constructor as maxDistance = chunks * 16.
     * Vanilla: monsters = 10, animals = 5-8, misc = 5.
     */
    public static final int MIN_TRACKING_RANGE_CHUNKS = 10;

    /**
     * Minimum tracking distance in BLOCKS enforced in EntityTracker.getMaxTrackDistance().
     * This fires after adjustTrackingDistance() applies the view-distance multiplier,
     * so it's the hard floor regardless of server view distance setting.
     * 128 blocks = 8 chunks.
     */
    public static final int MIN_TRACKING_DISTANCE_BLOCKS = 128;

    // Registry-ID based overrides for mobs added in 1.21.11 (not in 1.21.1 compile mappings)
    private static final Map<String, Double> REGISTRY_SCALES = new HashMap<>();
    static {
        REGISTRY_SCALES.put("minecraft:nautilus",        0.40);
        REGISTRY_SCALES.put("minecraft:zombie_nautilus", 0.40);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("RealSize v1.1.0 loaded — EntityTracker floor mixin active, small mobs won't cull");

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof LivingEntity living)) return;

            // Check registry-ID overrides first (for 1.21.11 mobs not in 1.21.1 mappings)
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            double scale;
            if (REGISTRY_SCALES.containsKey(entityId)) {
                scale = REGISTRY_SCALES.get(entityId);
            } else {
                scale = getScaleStatic(entity.getType());
            }
            if (scale == 1.0) return;

            // Clamp to safe range
            final double finalScale = Math.max(FLOOR, Math.min(CAP, scale));

            // Apply visual + hitbox scale
            applyModifier(living, Attributes.SCALE, ID_SCALE,
                    finalScale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            // Large mobs: step height boost so they can navigate terrain
            if (finalScale > 1.10) {
                applyModifier(living, Attributes.STEP_HEIGHT, ID_STEP_HEIGHT,
                        0.5, AttributeModifier.Operation.ADD_VALUE);
            }
        });
    }

    private void applyModifier(LivingEntity living,
                                Holder<Attribute> attribute,
                                Identifier id, double value,
                                AttributeModifier.Operation operation) {
        AttributeInstance inst = living.getAttribute(attribute);
        if (inst == null) return;
        if (inst.getModifier(id) != null) return;
        inst.addPermanentModifier(new AttributeModifier(id, value, operation));
    }

    /**
     * Public static accessor used by {@link xyz.pyrehaven.realsize.mixin.EntityTypeTrackingMixin}.
     * Returns the raw (unclamped) scale for the given entity type.
     * Returns 1.0 for unmodified mobs.
     */
    public static double getScaleStatic(EntityType<?> type) {
        return getScale(type);
    }

    /**
     * Public static accessor used by {@link xyz.pyrehaven.realsize.mixin.EntityTrackerMixin}.
     * Checks REGISTRY_SCALES first (for 1.21.11-only mobs), then falls back to getScale().
     */
    public static double getScaleForId(String entityId, EntityType<?> type) {
        if (REGISTRY_SCALES.containsKey(entityId)) {
            return REGISTRY_SCALES.get(entityId);
        }
        return getScale(type);
    }

    /**
     * Returns the desired scale for each mob type.
     * 1.0 = unchanged (humanoids, fictional mobs, anything already correct).
     * Values are pre-clamped reasoning; onInitialize clamps again as safety net.
     */
    private static double getScale(EntityType<?> type) {

        // ── ARTHROPODS ──────────────────────────────────────────────────────────
        if (type == EntityType.SPIDER)           return 0.26;
        if (type == EntityType.CAVE_SPIDER)      return 0.20;
        if (type == EntityType.SILVERFISH)       return 0.22;
        if (type == EntityType.ENDERMITE)        return 0.22;

        // ── INSECTS ─────────────────────────────────────────────────────────────
        if (type == EntityType.BEE)              return 0.25;

        // ── BATS ────────────────────────────────────────────────────────────────
        if (type == EntityType.BAT)              return 0.24;

        // ── AMPHIBIANS ──────────────────────────────────────────────────────────
        if (type == EntityType.FROG)             return 0.28;
        if (type == EntityType.TADPOLE)          return 0.22;
        if (type == EntityType.AXOLOTL)          return 0.55;

        // ── FISH ────────────────────────────────────────────────────────────────
        if (type == EntityType.TROPICAL_FISH)    return 0.22;
        if (type == EntityType.PUFFERFISH)       return 0.43;
        if (type == EntityType.COD)              return 0.50;
        if (type == EntityType.SALMON)           return 0.70;

        // ── CEPHALOPODS & MOLLUSCS ──────────────────────────────────────────────
        if (type == EntityType.SQUID)            return 0.40;
        if (type == EntityType.GLOW_SQUID)       return 0.40;

        // ── REPTILES ────────────────────────────────────────────────────────────
        if (type == EntityType.TURTLE)           return 0.55;

        // ── BIRDS ───────────────────────────────────────────────────────────────
        if (type == EntityType.CHICKEN)          return 0.64;
        if (type == EntityType.PARROT)           return 0.45;

        // ── SMALL MAMMALS ───────────────────────────────────────────────────────
        if (type == EntityType.RABBIT)           return 0.50;
        if (type == EntityType.CAT)              return 0.45;
        if (type == EntityType.FOX)              return 0.50;
        if (type == EntityType.ARMADILLO)        return 0.38;

        // ── FICTIONAL SMALL ─────────────────────────────────────────────────────
        if (type == EntityType.ALLAY)            return 0.42;
        if (type == EntityType.VEX)              return 0.30;

        // ── MEDIUM MAMMALS ──────────────────────────────────────────────────────
        if (type == EntityType.OCELOT)           return 0.64;
        if (type == EntityType.GOAT)             return 0.65;
        if (type == EntityType.WOLF)             return 0.88;
        if (type == EntityType.PIG)              return 0.95;
        if (type == EntityType.SHEEP)            return 0.75;
        if (type == EntityType.PANDA)            return 0.92;

        // ── LARGE MAMMALS ───────────────────────────────────────────────────────
        if (type == EntityType.POLAR_BEAR)       return 1.05;
        if (type == EntityType.HOGLIN)           return 0.85;
        if (type == EntityType.ZOGLIN)           return 0.85;
        if (type == EntityType.DONKEY)           return 0.88;
        if (type == EntityType.MULE)             return 0.93;
        if (type == EntityType.HORSE)            return 1.05;
        if (type == EntityType.SKELETON_HORSE)   return 1.05;
        if (type == EntityType.ZOMBIE_HORSE)     return 1.05;
        if (type == EntityType.LLAMA)            return 0.92;
        if (type == EntityType.TRADER_LLAMA)     return 0.92;
        if (type == EntityType.CAMEL)            return 1.10;
        if (type == EntityType.SNIFFER)          return 0.94;

        // ── AQUATIC LARGE ───────────────────────────────────────────────────────
        if (type == EntityType.DOLPHIN)          return 1.30;
        if (type == EntityType.GUARDIAN)         return 0.70;
        if (type == EntityType.ELDER_GUARDIAN)   return 1.35;

        // ── NETHER REAL-BASED ───────────────────────────────────────────────────
        if (type == EntityType.PIGLIN_BRUTE)     return 1.05;

        // ── CONSTRUCTED / BOSS ──────────────────────────────────────────────────
        if (type == EntityType.IRON_GOLEM)       return 1.20;
        if (type == EntityType.RAVAGER)          return 1.15;

        // ── EVERYTHING ELSE → unchanged ─────────────────────────────────────────
        return 1.0;
    }
}
