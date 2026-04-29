package xyz.pyrehaven.realsize;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealSizeMod implements ModInitializer {
    public static final String MOD_ID = "realsize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Identifier ID_SCALE = Identifier.fromNamespaceAndPath(MOD_ID, "scale");
    private static final Identifier ID_STEP_HEIGHT = Identifier.fromNamespaceAndPath(MOD_ID, "step_height");

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
            applyModifier(living, Attributes.SCALE, ID_SCALE,
                    clampedScale - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            if (RealSizeLogic.shouldBoostStepHeight(entityId, 1.0D, current)) {
                applyModifier(living, Attributes.STEP_HEIGHT, ID_STEP_HEIGHT,
                        current.stepHeightBoostAmount(), AttributeModifier.Operation.ADD_VALUE);
            }
        });
    }

    private void applyModifier(LivingEntity living,
                               Holder<Attribute> attribute,
                               Identifier id,
                               double value,
                               AttributeModifier.Operation operation) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }

        instance.addPermanentModifier(new AttributeModifier(id, value, operation));
    }

    public static double rawScaleForEntity(String entityId, EntityType<?> type) {
        return RealSizeLogic.scaleForEntity(entityId, 1.0D, RealSizeConfigManager.currentConfig());
    }

    public static double getScaleStatic(EntityType<?> type) {
        return rawScaleForEntity(entityId(type), type);
    }

    public static String entityId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }
}
