package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin({Zombie.class, Drowned.class, Husk.class})
abstract class ZombieSpawnMixin {
    @WrapOperation(method = "finalizeSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean realsize$prepareCompanion(ServerLevelAccessor level, Entity entity, Operation<Boolean> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null) return original.call(level, entity);
        preparation.defer(() -> original.call(level, entity));
        return true;
    }
}
