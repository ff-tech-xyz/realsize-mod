package xyz.pyrehaven.realsize;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;

public final class ChunkSpawnGameTests {
    @GameTest(setupTicks = 4200, maxTicks = 2000)
    public void generationRetriesAfterCompleteComposedFinalization(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(-256, 6, -256));
        var player = helper.makeMockServerPlayerInLevel();
        player.snapTo(target.getX() + 40.0, target.getY(), target.getZ());
        SpawnGameTests.forceArea(helper, target, true);
        SpawnGameTests.naturalRoom(helper, target, true);
        SpawnGameTests.naturalRoom(helper, target, false);
        helper.startSequence().thenWaitUntil(() -> {
            var chunk = ChunkPos.containing(target);
            // GameTestServer runs ticks without sleeping. Give entity-storage I/O
            // wall-clock time instead of burning the whole wait in <300 ms.
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
        var obstacles = new ArrayList<BlockPos>();
        var evidence = new StringBuilder();
        try {
            SpawnGameTests.reload(helper, Map.of("minecraft:mooshroom", 1.45));
            for (int phase = 0; phase < 2; phase++) {
                boolean reject = phase == 0;
                NaturalSpawnProbe.reset();
                SpawnGameTests.registered.clear();
                NaturalSpawnProbe.inner = mob -> mob.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(
                        Identifier.parse("spawn-test:inner"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                NaturalSpawnProbe.outer = mob -> {
                    mob.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(
                            Identifier.parse("spawn-test:outer"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    if (reject) {
                        var obstacle = mob.blockPosition().above(2);
                        obstacles.add(obstacle);
                        level.setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
                    }
                };
                var generationRandom = RandomSource.create(71000);
                for (int seed = 71000; seed < 71080; seed++) {
                    level.getRandom().setSeed(seed);
                    NaturalSpawner.spawnMobsForChunkGeneration(level, target, ChunkPos.containing(target), generationRandom);
                    obstacles.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
                    obstacles.clear();
                }
                helper.assertTrue(NaturalSpawnProbe.innerCalls > 4 && NaturalSpawnProbe.innerCalls == NaturalSpawnProbe.outerCalls,
                        "Both composed call-site wrappers must execute once per real finalized candidate; inner="
                                + NaturalSpawnProbe.innerCalls + " outer=" + NaturalSpawnProbe.outerCalls);
                helper.assertTrue(NaturalSpawnProbe.finalized.stream().map(row -> row.mob().position()).distinct().count() > 1,
                        "Failed chunkgen candidates must retain coordinate-update retries");
                if (reject) {
                    helper.assertTrue(SpawnGameTests.registered.isEmpty(), "Outer-wrapper collision must prevent all chunkgen registration");
                    for (var row : NaturalSpawnProbe.finalized) {
                        helper.assertTrue(row.mob().isRemoved() && ((AgeableMob.AgeableMobGroupData) row.group()).getGroupSize() == 0,
                                "Chunkgen rejection must discard candidate without committing group");
                    }
                } else {
                    helper.assertTrue(!SpawnGameTests.registered.isEmpty(), "Open chunkgen must register actual successes");
                    for (var entity : SpawnGameTests.registered) {
                        var mob = (net.minecraft.world.entity.Mob) entity;
                        helper.assertTrue(mob.getAttribute(Attributes.SCALE).getModifier(Identifier.parse("spawn-test:inner")) != null
                                && mob.getAttribute(Attributes.SCALE).getModifier(Identifier.parse("spawn-test:outer")) != null,
                                "Both foreign finalizer modifiers must survive the gate");
                        helper.assertTrue(Math.abs(mob.getBoundingBox().getYsize() - mob.getDimensions(mob.getPose()).height()) < 1e-5
                                && level.noCollision(mob, mob.getBoundingBox()), "Accepted final dimensions must include both foreign wrappers");
                    }
                }
                evidence.append("phase=").append(phase).append(" finalized=").append(NaturalSpawnProbe.innerCalls)
                        .append(" registrations=").append(SpawnGameTests.registered.size()).append('\n');
                SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
            }
            Files.writeString(file.getParent().resolve("chunkgen-coverage.txt"), evidence);
        } finally {
            obstacles.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
            SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
            SpawnGameTests.registered.clear();
            NaturalSpawnProbe.reset();
            Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }
}
