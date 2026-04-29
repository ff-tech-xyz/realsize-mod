package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pyrehaven.realsize.RealSizeConfig;
import xyz.pyrehaven.realsize.RealSizeConfigManager;
import xyz.pyrehaven.realsize.RealSizeLogic;
import xyz.pyrehaven.realsize.RealSizeMod;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class EntityTrackerMixin {

    @Final
    @Shadow
    Entity entity;

    @Inject(method = "getEffectiveRange()I", at = @At("RETURN"), cancellable = true)
    private void realsize_enforceMinTrackDistance(CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity)) {
            return;
        }

        RealSizeConfig config = RealSizeConfigManager.currentConfig();
        if (RealSizeLogic.shouldBoostTracking(RealSizeMod.entityId(entity.getType()), 1.0D, config)) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), config.minTrackingDistanceBlocks()));
        }
    }
}
