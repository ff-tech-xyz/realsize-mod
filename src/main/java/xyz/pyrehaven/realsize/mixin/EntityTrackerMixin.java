package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pyrehaven.realsize.RealSizeMod;

/**
 * Hooks ServerChunkCache$EntityTracker.getMaxTrackDistance() to enforce
 * a minimum tracking distance (in blocks) for small scaled mobs.
 *
 * This fires AFTER adjustTrackingDistance() already applied the server's view-distance
 * multiplier, so it's the true last word on whether a player receives updates.
 *
 * MIN_TRACKING_DISTANCE_BLOCKS = 128 blocks (8 chunks).
 * This overrides even low view-distance settings for affected mobs.
 */
@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$EntityTracker")
public class EntityTrackerMixin {

    @Final
    @Shadow
    Entity entity;

    @Inject(method = "getMaxTrackDistance()I", at = @At("RETURN"), cancellable = true)
    private void realsize_enforceMinTrackDistance(CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity)) return;

        String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType()).toString();

        double scale = RealSizeMod.getScaleForId(entityId, entity.getType());
        double clamped = Math.max(RealSizeMod.FLOOR, Math.min(RealSizeMod.CAP, scale));

        if (clamped < RealSizeMod.TRACKING_RANGE_THRESHOLD) {
            int enforced = Math.max(cir.getReturnValue(), RealSizeMod.MIN_TRACKING_DISTANCE_BLOCKS);
            cir.setReturnValue(enforced);
        }
    }
}
