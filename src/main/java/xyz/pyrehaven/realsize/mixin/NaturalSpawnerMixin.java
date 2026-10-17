package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.util.RandomSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

// The configuration plugin adds only the missing post-finalization failure edge.
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @WrapMethod(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V")
    private static void realsize$tickingScope(MobCategory category, ServerLevel level, ChunkAccess chunk, BlockPos pos,
            NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback callback, Operation<Void> original) {
        var enclosing = SpawnPreparation.current();
        try {
            original.call(category, level, chunk, pos, predicate, callback);
        } finally {
            SpawnPreparation.closeTo(enclosing);
        }
    }

    @WrapMethod(method = "spawnMobsForChunkGeneration")
    private static void realsize$generationScope(ServerLevelAccessor level, BlockPos pos, ChunkPos chunk,
            RandomSource random, Operation<Void> original) {
        var enclosing = SpawnPreparation.current();
        try {
            original.call(level, pos, chunk, random);
        } finally {
            SpawnPreparation.closeTo(enclosing);
        }
    }
}
