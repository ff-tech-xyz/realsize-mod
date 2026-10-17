package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(Husk.class)
abstract class HuskSpawnMixin {
    @WrapOperation(method = "finalizeSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void realsize$prepareSibling(ServerLevelAccessor level, Entity entity, Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) original.call(level, entity);
        else preparation.defer(() -> original.call(level, entity));
    }
}
