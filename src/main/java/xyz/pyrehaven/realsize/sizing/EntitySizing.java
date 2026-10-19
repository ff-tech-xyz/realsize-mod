package xyz.pyrehaven.realsize.sizing;

import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.monster.Guardian;
import xyz.pyrehaven.realsize.config.ResolvedSettings;
import xyz.pyrehaven.realsize.config.RealSizeConfig;

public final class EntitySizing {
    private static final Identifier SCALE_ID = Identifier.fromNamespaceAndPath("realsize", "scale");
    private static final Identifier STEP_ID = Identifier.fromNamespaceAndPath("realsize", "step_height");
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

    /** Scale only vanilla's existing adult spawn check; finalization stays vanilla. */
    public net.minecraft.world.phys.AABB spawnBounds(net.minecraft.world.entity.EntityType<?> type,
            net.minecraft.world.phys.AABB original) {
        double multiplier = policy.multiplier(settings.scale(type), type.getCategory() == MobCategory.MONSTER, 1.0);
        if (multiplier == 1.0) return original;
        double centerX = (original.minX + original.maxX) * 0.5;
        double centerZ = (original.minZ + original.maxZ) * 0.5;
        double halfX = original.getXsize() * multiplier * 0.5;
        double halfZ = original.getZsize() * multiplier * 0.5;
        return new net.minecraft.world.phys.AABB(centerX - halfX, original.minY, centerZ - halfZ,
                centerX + halfX, original.minY + original.getYsize() * multiplier, centerZ + halfZ);
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
        reconcileMovement(living);
    }

    private void reconcileMovement(LivingEntity living) {
        var step = living.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) {
            return;
        }
        Double rule = settings.scale(living.getType());
        if (!(living instanceof Mob) || aquatic(living) || rule == null || rule == 1.0
                || !settings.config().proportionalStepHeight()) {
            step.removeModifier(STEP_ID);
            return;
        }
        step.removeModifier(STEP_ID);
        double baseline = step.getValue();
        double desired = policy.stepHeight(baseline, living.getScale() * living.getAgeScale(),
                settings.config().maxStepHeight());
        if (desired == baseline) {
            return;
        }
        // A total multiplier adds exactly the desired effective difference even with
        // foreign ADD_MULTIPLIED_BASE/TOTAL modifiers; a flat ADD_VALUE would not.
        double amount = desired / baseline - 1.0;
        step.addTransientModifier(new AttributeModifier(STEP_ID, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static boolean aquatic(LivingEntity living) {
        // Guardians and zombie nautiluses are MONSTER, tadpoles are MISC.
        return switch (living.getType().getCategory()) {
            case AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT -> true;
            default -> living instanceof net.minecraft.world.entity.animal.fish.WaterAnimal
                    || living instanceof Guardian || living instanceof AbstractNautilus;
        };
    }
}
