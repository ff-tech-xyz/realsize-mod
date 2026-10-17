package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.RealSizeMod;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @WrapOperation(method = {"isValidSpawnPostitionForType", "spawnMobsForChunkGeneration"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"),
            require = 2, expect = 2, allow = 2)
    private static AABB realsize$adultBounds(EntityType<?> type, double x, double y, double z, Operation<AABB> original) {
        return RealSizeMod.currentSizing().spawnBounds(type, original.call(type, x, y, z));
    }
}
