package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(Chicken.class)
abstract class ChickenSpawnMixin {
    @WrapMethod(method = "setChickenJockey")
    private void realsize$prepareJockey(boolean jockey, Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) original.call(jockey);
        else preparation.defer(() -> original.call(jockey));
    }
}
