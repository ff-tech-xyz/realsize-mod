package xyz.pyrehaven.realsize;

import java.nio.file.Files;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;

public final class ChunkSpawnGameTests {
    @GameTest(setupTicks = 4200, maxTicks = 2000)
    public void generationChecksAdultBoundsBeforeFinalization(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(-256, 6, -256));
        var player = helper.makeMockServerPlayerInLevel();
        player.snapTo(target.getX() + 40.0, target.getY(), target.getZ());
        SpawnGameTests.forceArea(helper, target, true);
        SpawnGameTests.naturalRoom(helper, target, true);
        helper.startSequence().thenWaitUntil(() -> {
            var chunk = ChunkPos.containing(target);
            // The accelerated test ticks must leave wall-clock time for entity-storage I/O.
            if (!level.areEntitiesLoaded(chunk.pack())) java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
            helper.assertTrue(level.areEntitiesLoaded(chunk.pack()), "Chunkgen fixture must finish entity loading");
            helper.assertTrue(level.getMaxLocalRawBrightness(target) > 8, "Chunkgen fixture lighting must finish");
        }).thenExecute(() -> {
            try {
                checks(helper, target);
            } catch (net.minecraft.gametest.framework.GameTestAssertException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                player.discard();
                SpawnGameTests.forceArea(helper, target, false);
            }
        }).thenSucceed();
    }

    private static void checks(GameTestHelper helper, BlockPos target) throws Exception {
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var level = helper.getLevel();
        var lateObstacles = new java.util.ArrayList<BlockPos>();
        int lateCollisions = 0;
        try {
            SpawnGameTests.reload(helper, Map.of("minecraft:mooshroom", 1.45));
            for (int phase = 0; phase < 3; phase++) {
                NaturalSpawnProbe.reset();
                SpawnGameTests.registered.clear();
                if (phase == 1) SpawnGameTests.naturalRoom(helper, target, false);
                if (phase == 2) {
                    NaturalSpawnProbe.outer = mob -> {
                        var obstacle = mob.blockPosition();
                        lateObstacles.add(obstacle);
                        level.setBlockAndUpdate(obstacle, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    };
                }
                var generationRandom = RandomSource.create(71000);
                for (int seed = 71000; seed < 71080; seed++) {
                    level.getRandom().setSeed(seed);
                    int before = SpawnGameTests.registered.size();
                    NaturalSpawner.spawnMobsForChunkGeneration(level, target, ChunkPos.containing(target), generationRandom);
                    for (var entity : SpawnGameTests.registered.subList(before, SpawnGameTests.registered.size())) {
                        if (!level.noCollision(entity, entity.getBoundingBox())) lateCollisions++;
                    }
                    lateObstacles.forEach(pos -> level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
                    lateObstacles.clear();
                }
                if (phase == 0) {
                    helper.assertTrue(SpawnGameTests.registered.isEmpty(), "Early adult bounds must reject the short roof");
                    helper.assertTrue(NaturalSpawnProbe.finalized.isEmpty(), "Rejected adult bounds must not run finalization");
                } else {
                    helper.assertTrue(!SpawnGameTests.registered.isEmpty(), "Open chunkgen must register actual successes");
                    helper.assertTrue(NaturalSpawnProbe.innerCalls == SpawnGameTests.registered.size()
                            && NaturalSpawnProbe.innerCalls == NaturalSpawnProbe.outerCalls,
                            "Original composed finalization runs once per accepted candidate");
                    helper.assertTrue(NaturalSpawnProbe.finalized.stream().map(row -> row.mob().position()).distinct().count() > 1,
                            "Chunk generation retains its coordinate changes");
                }
                SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
            }
            helper.assertTrue(lateCollisions > 0, "Vanilla must accept candidates made obstructed by a late finalizer without a new recheck");
        } finally {
            lateObstacles.forEach(pos -> level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
            SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
            SpawnGameTests.registered.clear();
            NaturalSpawnProbe.reset();
            Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }
}
