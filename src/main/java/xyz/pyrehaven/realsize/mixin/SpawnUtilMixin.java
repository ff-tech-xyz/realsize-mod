package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.RealSizeMod;

@Mixin(net.minecraft.util.SpawnUtil.class)
abstract class SpawnUtilMixin {
    @WrapOperation(method = "trySpawnMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"),
            require = 1, expect = 1, allow = 1)
    private static AABB realsize$adultBounds(EntityType<?> type, double x, double y, double z, Operation<AABB> original) {
        return RealSizeMod.currentSizing().spawnBounds(type, original.call(type, x, y, z));
    }
}
