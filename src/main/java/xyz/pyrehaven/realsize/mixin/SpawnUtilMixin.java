package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(SpawnUtil.class)
abstract class SpawnUtilMixin {
    @WrapMethod(method = "trySpawnMob")
    private static <T extends Mob> Optional<T> realsize$scope(EntityType<T> type, EntitySpawnReason reason, ServerLevel level,
            BlockPos pos, int attempts, int rangeXZ, int rangeY, SpawnUtil.Strategy strategy, boolean collisions,
            Operation<Optional<T>> original) {
        var enclosing = SpawnPreparation.current();
        try {
            return original.call(type, reason, level, pos, attempts, rangeXZ, rangeY, strategy, collisions);
        } finally {
            SpawnPreparation.closeTo(enclosing);
        }
    }

    @WrapOperation(method = "trySpawnMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"))
    private static Entity realsize$prepare(EntityType<?> type, ServerLevel level, PostSpawnProcessor<?> processor,
            BlockPos pos, EntitySpawnReason reason, boolean down, boolean up, Operation<Entity> original) {
        SpawnPreparation.begin(null);
        var result = original.call(type, level, processor, pos, reason, down, up);
        if (result == null) SpawnPreparation.current().abort();
        return result;
    }

    @WrapOperation(method = "trySpawnMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;checkSpawnObstruction(Lnet/minecraft/world/level/LevelReader;)Z"))
    private static boolean realsize$clear(Mob mob, LevelReader level, Operation<Boolean> original) {
        return original.call(mob, level) && SpawnPreparation.current().clear((ServerLevelAccessor) level);
    }

    @WrapOperation(method = "trySpawnMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void realsize$commit(ServerLevel level, Entity entity, Operation<Void> original) {
        SpawnPreparation.current().commit();
        original.call(level, entity);
    }

    @WrapOperation(method = "trySpawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private static void realsize$abort(Mob mob, Operation<Void> original) {
        SpawnPreparation.current().abort();
    }
}
