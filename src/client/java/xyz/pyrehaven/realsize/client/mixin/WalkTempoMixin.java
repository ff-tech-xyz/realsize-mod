package xyz.pyrehaven.realsize.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.creaking.Creaking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.pyrehaven.realsize.client.RealSizeClient;

@Mixin(value = {LivingEntity.class, Camel.class, Frog.class, Creaking.class}, remap = false)
abstract class WalkTempoMixin {
    @ModifyArg(method = "updateWalkAnimation(F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/WalkAnimationState;update(FFF)V"),
            index = 2, require = 1, expect = 1, allow = 1)
    private float realsize$legTempo(float tempo) {
        if (!((Object) this instanceof Mob mob) || !RealSizeClient.walkTempoFixEnabled()) {
            return tempo;
        }
        // Attribute scale excludes age; vanilla already supplies the baby tempo.
        float scale = mob.getScale();
        return scale > 0 ? Math.min(tempo / scale, 6.0f) : tempo;
    }
}
