package xyz.pyrehaven.realsize.mixin;

import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pyrehaven.realsize.RealSizeConfig;
import xyz.pyrehaven.realsize.RealSizeConfigManager;
import xyz.pyrehaven.realsize.RealSizeLogic;
import xyz.pyrehaven.realsize.RealSizeMod;

@Mixin(EntityType.class)
public class EntityTypeTrackingMixin {

    @Inject(method = "getMaxTrackDistance()I", at = @At("RETURN"), cancellable = true)
    private void realsize_boostSmallMobRange(CallbackInfoReturnable<Integer> cir) {
        @SuppressWarnings("unchecked")
        EntityType<?> self = (EntityType<?>) (Object) this;
        RealSizeConfig config = RealSizeConfigManager.currentConfig();

        if (RealSizeLogic.shouldBoostTracking(RealSizeMod.entityId(self), 1.0D, config)) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), config.minTrackingRangeChunks()));
        }
    }
}
