package xyz.pyrehaven.realsize.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.client.RealSizeClient;

@Mixin(Entity.class)
public abstract class EntityRenderDistanceMixin {
    @ModifyReturnValue(method = "shouldRenderAtSqrDistance(D)Z", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private boolean realsize$unscaledDistance(boolean original, double distance) {
        if (original || !RealSizeClient.renderDistanceFixEnabled()
                || !((Object) this instanceof LivingEntity entity)) {
            return original;
        }
        float scale = entity.getScale();
        if (!(scale > 0.0F && scale < 1.0F)) {
            return original;
        }
        // Undo only SCALE: retain pose, baby and intrinsic dimensions, not a server rule lookup.
        double size = entity.getBoundingBox().getSize() / scale;
        if (Double.isNaN(size)) {
            size = 1.0;
        }
        double range = size * 64.0 * Entity.getViewScale();
        return distance < range * range;
    }
}
