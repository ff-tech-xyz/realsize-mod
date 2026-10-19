package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MoveControl.class)
abstract class TurnRateMixin {
    @Shadow @Final protected Mob mob;

    @ModifyArg(method = "tick()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/control/MoveControl;rotlerp(FFF)F"),
            index = 2, require = 1, expect = 1, allow = 1)
    private float realsize$gentleTurn(float maxTurn) {
        float scale = mob.getScale();
        return !mob.level().isClientSide() && scale > 0 && scale < 1
                ? Math.min(maxTurn, 35.0f) : maxTurn;
    }
}
