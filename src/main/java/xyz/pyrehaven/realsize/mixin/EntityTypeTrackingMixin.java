package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pyrehaven.realsize.RealSizeMod;

/**
 * Boosts the server-side entity max track distance for small mobs so they are never
 * culled from client view even when their hitbox is tiny.
 *
 * In 26.1.2 the old getMaxTrackDistance() method became the official
 * accessor clientTrackingRange(). It still returns distance in CHUNKS, not blocks.
 * Vanilla: monsters = 10 chunks, animals = 5-8 chunks, misc = 5 chunks.
 * We boost small mobs to at least MIN_TRACKING_RANGE_CHUNKS (8 chunks = 128 blocks).
 */
@Mixin(EntityType.class)
public class EntityTypeTrackingMixin {

    @Inject(method = "clientTrackingRange()I", at = @At("RETURN"), cancellable = true)
    private void realsize_boostSmallMobRange(CallbackInfoReturnable<Integer> cir) {
        @SuppressWarnings("unchecked")
        EntityType<?> self = (EntityType<?>) (Object) this;
        double scale = RealSizeMod.getScaleStatic(self);
        if (scale < RealSizeMod.TRACKING_RANGE_THRESHOLD) {
            int boosted = Math.max(cir.getReturnValue(), RealSizeMod.MIN_TRACKING_RANGE_CHUNKS);
            cir.setReturnValue(boosted);
        }
    }
}
