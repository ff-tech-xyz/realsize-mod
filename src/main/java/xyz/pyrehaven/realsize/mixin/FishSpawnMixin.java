package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(AbstractSchoolingFish.class)
abstract class FishSpawnMixin {
    @WrapMethod(method = "addFollower")
    private void realsize$prepareFollower(Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) original.call();
        else preparation.changeCount(this, 1, () -> original.call());
    }

    @WrapMethod(method = "removeFollower")
    private void realsize$prepareFollowerRemoval(Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) original.call();
        else preparation.changeCount(this, -1, () -> original.call());
    }

    @ModifyExpressionValue(method = {"hasFollowers", "canBeFollowed", "addFollowers"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/animal/fish/AbstractSchoolingFish;schoolSize:I"))
    private int realsize$pendingSchoolSize(int actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : preparation.count(this, actual);
    }
}
