package xyz.pyrehaven.realsize.sizing;

import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.pyrehaven.realsize.config.ResolvedSettings;
import xyz.pyrehaven.realsize.config.RealSizeConfig;

public final class EntitySizing {
    private static final Identifier SCALE_ID = Identifier.fromNamespaceAndPath("realsize", "scale");
    private final ResolvedSettings settings;
    private final SizingPolicy policy;
    private final Consumer<String> diagnostics;

    public EntitySizing(ResolvedSettings settings, Consumer<String> diagnostics) {
        this.settings = settings;
        this.diagnostics = diagnostics;
        var config = settings.config();
        this.policy = new SizingPolicy(config.floor(), config.hostileFloor(), config.cap());
    }

    public RealSizeConfig config() {
        return settings.config();
    }

    public void onLoad(Entity entity) {
        if (entity instanceof LivingEntity living) {
            // pack exposes every instantiated attribute, including non-syncable legacy data.
            // Its modifier lists contain only permanent modifiers, not transient ones.
            for (var packed : living.getAttributes().pack()) {
                for (var modifier : packed.modifiers()) {
                    if (modifier.id().getNamespace().equals("realsize")) {
                        living.getAttribute(packed.attribute()).removeModifier(modifier.id());
                    }
                }
            }
        }
        reconcile(entity);
    }

    public void reconcile(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        var scale = living.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        double multiplier = policy.multiplier(settings.scale(living.getType()),
                living.getType().getCategory() == MobCategory.MONSTER, living.getAgeScale());
        if (multiplier == 1.0) {
            scale.removeModifier(SCALE_ID);
        } else {
            var current = scale.getModifier(SCALE_ID);
            if (current == null || current.amount() != multiplier - 1.0
                    || current.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                scale.removeModifier(SCALE_ID);
                scale.addTransientModifier(new AttributeModifier(SCALE_ID, multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                double finalScale = living.getScale() * living.getAgeScale();
                double floor = living.getType().getCategory() == MobCategory.MONSTER
                        ? settings.config().hostileFloor() : settings.config().floor();
                if (finalScale < floor - 0.00001 || finalScale > settings.config().cap() + 0.00001) {
                    diagnostics.accept("RealSize bounds overridden by base/foreign scale on "
                            + living.getStringUUID() + ": final adult-relative size " + finalScale);
                }
            }
        }
    }
}
