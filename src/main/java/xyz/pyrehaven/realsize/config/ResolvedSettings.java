package xyz.pyrehaven.realsize.config;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public final class ResolvedSettings {
    private final RealSizeConfig config;
    private final Map<EntityType<?>, Double> scales;

    private ResolvedSettings(RealSizeConfig config, Map<EntityType<?>, Double> scales) {
        this.config = config;
        this.scales = Collections.unmodifiableMap(new IdentityHashMap<>(scales));
    }

    public static ResolvedSettings resolve(RealSizeConfig config, Registry<EntityType<?>> registry,
            Consumer<String> warnings) {
        Map<EntityType<?>, Double> scales = new IdentityHashMap<>();
        var rules = new TreeMap<>(config.entityScales());
        rules.forEach((selector, scale) -> {
            if (!selector.startsWith("#")) {
                registry.getOptional(Identifier.parse(selector)).ifPresentOrElse(type -> {
                    if (eligible(type)) {
                        scales.put(type, scale);
                    }
                }, () -> warnings.accept("entityScales[" + selector + "]: unknown entity ID; rule retained"));
            }
        });
        var exactTypes = new IdentityHashMap<>(scales);
        Map<EntityType<?>, String> tagOwners = new IdentityHashMap<>();
        rules.forEach((selector, scale) -> {
            if (selector.startsWith("#")) {
                registry.get(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(selector.substring(1))))
                        .ifPresentOrElse(tag -> tag.forEach(holder -> {
                            var type = holder.value();
                            if (!eligible(type) || exactTypes.containsKey(type)) {
                                return;
                            }
                            var previous = scales.putIfAbsent(type, scale);
                            if (previous != null && Double.compare(previous, scale) != 0) {
                                throw new IllegalArgumentException("entityScales: conflicting tags "
                                        + tagOwners.get(type) + " and " + selector + " for " + registry.getKey(type));
                            }
                            tagOwners.putIfAbsent(type, selector);
                        }), () -> warnings.accept("entityScales[" + selector + "]: unknown entity tag; rule retained"));
            }
        });
        return new ResolvedSettings(config, scales);
    }

    public RealSizeConfig config() {
        return config;
    }

    /** Returns the requested scale, including neutral 1.0, or null for no eligible rule. */
    public Double scale(EntityType<?> type) {
        return scales.get(type);
    }

    private static boolean eligible(EntityType<?> type) {
        // Living types register default attributes; EntityType.getBaseClass() is always Entity.class.
        return type != EntityTypes.PLAYER && DefaultAttributes.hasSupplier(type);
    }
}
