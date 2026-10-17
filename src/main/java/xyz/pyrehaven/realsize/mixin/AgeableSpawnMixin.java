package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(AgeableMob.AgeableMobGroupData.class)
abstract class AgeableSpawnMixin {
    @WrapMethod(method = "increaseGroupSizeByOne")
    private void realsize$acceptGroup(Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) original.call();
        else preparation.changeCount(this, 1, () -> original.call());
    }

    @ModifyReturnValue(method = "getGroupSize", at = @At("RETURN"))
    private int realsize$projectGroup(int actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : preparation.count(this, actual);
    }
}
