package xyz.pyrehaven.realsize;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealSizeMod implements ModInitializer {
    public static final String MOD_ID = "realsize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Identifier ID_SCALE = Identifier.of(MOD_ID, "scale");
    private static final Identifier ID_STEP_HEIGHT = Identifier.of(MOD_ID, "step_height");

    @Override
    public void onInitialize() {
        RealSizeConfig config = RealSizeConfigManager.loadFromConfigDir(FabricLoader.getInstance().getConfigDir());
        LOGGER.info("RealSize loaded with {} configured entity scales", config.entityScales().size());

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof LivingEntity living)) {
                return;
            }

            String entityId = entityId(entity.getType());
            double rawScale = rawScaleForEntity(entityId, entity.getType());
            if (rawScale == 1.0D) {
                return;
            }

            RealSizeConfig current = RealSizeConfigManager.currentConfig();
            double clampedScale = RealSizeLogic.clampScale(rawScale, current);
            applyModifier(living, EntityAttributes.GENERIC_SCALE, ID_SCALE,
                    clampedScale - 1.0D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            if (RealSizeLogic.shouldBoostStepHeight(entityId, 1.0D, current)) {
                applyModifier(living, EntityAttributes.GENERIC_STEP_HEIGHT, ID_STEP_HEIGHT,
                        current.stepHeightBoostAmount(), EntityAttributeModifier.Operation.ADD_VALUE);
            }
        });
    }

    private void applyModifier(LivingEntity living,
                               RegistryEntry<EntityAttribute> attribute,
                               Identifier id,
                               double value,
                               EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = living.getAttributeInstance(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }

        instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
    }

    public static double rawScaleForEntity(String entityId, EntityType<?> type) {
        return RealSizeLogic.scaleForEntity(entityId, 1.0D, RealSizeConfigManager.currentConfig());
    }

    public static double getScaleStatic(EntityType<?> type) {
        return rawScaleForEntity(entityId(type), type);
    }

    public static double getScaleForId(String entityId, EntityType<?> type) {
        return rawScaleForEntity(entityId, type);
    }

    public static String entityId(EntityType<?> type) {
        return Registries.ENTITY_TYPE.getId(type).toString();
    }
}
