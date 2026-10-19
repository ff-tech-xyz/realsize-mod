package xyz.pyrehaven.realsize;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import xyz.pyrehaven.realsize.config.RealSizeConfig;

public final class SpawnGameTests implements net.fabricmc.api.ModInitializer {
    static final java.util.List<net.minecraft.world.entity.Entity> registered = new java.util.ArrayList<>();

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> registered.add(entity));
    }

    @GameTest(setupTicks = 2400, maxTicks = 400)
    public void naturalLoopChecksAdultSizeBeforeFinalization(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(-96, 6, -96));
        var player = helper.makeMockServerPlayerInLevel();
        player.snapTo(target.getX() + 40.0, target.getY(), target.getZ());
        forceArea(helper, target, true);
        naturalRoom(helper, target, true);
        helper.startSequence().thenWaitUntil(() -> {
            for (int x = -20; x <= 20; x += 4) {
                for (int z = -20; z <= 20; z += 4) {
                    var chunk = net.minecraft.world.level.ChunkPos.containing(target.offset(x, 0, z));
                    helper.assertTrue(level.areEntitiesLoaded(chunk.pack()) && level.areEntitiesActuallyLoadedAndTicking(chunk),
                            "Natural fixture chunks must actually load and tick");
                }
            }
            helper.assertTrue(level.getMaxLocalRawBrightness(target) > 8, "Natural fixture lighting must finish");
        }).thenExecute(() -> {
            try {
                naturalChecks(helper, target);
            } catch (net.minecraft.gametest.framework.GameTestAssertException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                player.discard();
                forceArea(helper, target, false);
            }
        }).thenSucceed();
    }

    private static void naturalChecks(GameTestHelper helper, BlockPos target) throws Exception {
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var level = helper.getLevel();
        int[] candidates = {0};
        int[] successes = {0};
        try {
            reload(helper, Map.of("minecraft:mooshroom", 1.45));
            for (int phase = 0; phase < 2; phase++) {
                if (phase == 1) naturalRoom(helper, target, false);
                NaturalSpawnProbe.finalized.clear();
                for (int attempt = 0; attempt < 40; attempt++) {
                    level.getRandom().setSeed(9100 + phase * 100 + attempt);
                    net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(
                            net.minecraft.world.entity.MobCategory.CREATURE, level, level.getChunk(target), target,
                            (type, world, pos, chunk) -> { candidates[0]++; return true; }, (mob, chunk) -> {
                                successes[0]++;
                                try {
                                    helper.assertTrue(mob.getType() == EntityTypes.MOOSHROOM, "Mushroom biome must select mooshrooms");
                                    helper.assertTrue(level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(mob.position())),
                                            "Actual natural success must have finalized scaled collision clearance");
                                    helper.assertTrue(level.getEntity(mob.getUUID()) == mob, "Success callback must follow registration");
                                } finally {
                                    mob.discard();
                                }
                            });
                }
                if (phase == 0) {
                    helper.assertTrue(candidates[0] == 0, "Adult collision rejection must precede vanilla's extra predicate");
                    helper.assertTrue(successes[0] == 0, "Short space must have no accepted enlarged mooshrooms");
                    helper.assertTrue(NaturalSpawnProbe.finalized.isEmpty(),
                            "Scaled adult clearance must reject at vanilla's early check, before any finalization");
                }
            }
            helper.assertTrue(successes[0] > 0, "Open natural area must actually produce successes after failed candidates");
        } finally {
            NaturalSpawnProbe.finalized.clear();
            Files.write(file, original);
            executeReload(helper);
        }
    }

    static void forceArea(GameTestHelper helper, BlockPos target, boolean forced) {
        var min = net.minecraft.world.level.ChunkPos.containing(target.offset(-20, 0, -20));
        var max = net.minecraft.world.level.ChunkPos.containing(target.offset(20, 0, 20));
        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                helper.getLevel().setChunkForced(x, z, forced);
                if (forced) helper.getLevel().getChunk(x, z);
            }
        }
    }

    static void naturalRoom(GameTestHelper helper, BlockPos target, boolean shortRoof) throws Exception {
        var level = helper.getLevel();
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                level.setBlockAndUpdate(target.offset(x, -1, z), Blocks.MYCELIUM.defaultBlockState());
                for (int y = 0; y <= 5; y++) {
                    var state = Blocks.AIR.defaultBlockState();
                    if (y == 0) state = Blocks.LIGHT.defaultBlockState();
                    if (y == 1 && shortRoof) state = Blocks.STONE_SLAB.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP);
                    level.setBlockAndUpdate(target.offset(x, y, z), state);
                }
            }
        }
        if (shortRoof) {
            var min = target.offset(-20, -2, -20);
            var max = target.offset(20, 6, 20);
            level.getServer().getCommands().getDispatcher().execute("fillbiome " + min.getX() + " " + min.getY()
                    + " " + min.getZ() + " " + max.getX() + " " + max.getY() + " " + max.getZ()
                    + " minecraft:mushroom_fields", level.getServer().createCommandSourceStack());
        }
    }

    @GameTest(setupTicks = 2200)
    public void golemClearanceRejectsAndRetriesBeforeSuccess(GameTestHelper helper) throws Exception {
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        var level = helper.getLevel();
        try {
            reload(helper, Map.of("minecraft:iron_golem", 1.45));
            room(helper, target, 3);
            var rejected = SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED,
                    level, target, 2, 0, 0, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, true);
            helper.assertTrue(rejected.isEmpty(), "Enlarged golem must reject three-block roof before reporting success");
            int[] candidates = {0};
            SpawnUtil.Strategy retry = (world, below, state, above, aboveState) -> {
                if (!SpawnUtil.Strategy.LEGACY_IRON_GOLEM.canSpawnOn(world, below, state, above, aboveState)) return false;
                if (++candidates[0] == 2) room(helper, target, 4);
                return true;
            };
            var accepted = SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED,
                    level, target, 3, 0, 0, retry, true);
            helper.assertTrue(accepted.isPresent() && candidates[0] == 2, "Failed candidate must retry, then accept adequate space");
            var golem = accepted.orElseThrow();
            golem.setNoAi(true);
            golem.setNoGravity(true);
            golem.tick(); // Vanilla consumes the load-time scale attribute dirt on the entity tick.
            helper.assertTrue(level.getEntity(golem.getUUID()) == golem
                    && Math.abs(golem.getBoundingBox().getYsize() - 3.9150002002716064) < 0.00001
                    && level.noCollision(golem, golem.getBoundingBox()), "Success must register a final-sized clear golem");
            golem.discard();
            helper.succeed();
        } finally {
            Files.write(file, original);
            executeReload(helper);
        }
    }

    @GameTest(setupTicks = 2800)
    public void spawnUtilPreservesNeutralShrinkingAndDisabledCollisionChecks(GameTestHelper helper) throws Exception {
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        var level = helper.getLevel();
        try {
            for (double scale : new double[]{1.0, 0.5, 1.45}) {
                reload(helper, Map.of("minecraft:iron_golem", scale));
                room(helper, target, 3);
                var vanillaBounds = EntityTypes.IRON_GOLEM.getSpawnAABB(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                var configuredBounds = RealSizeMod.currentSizing().spawnBounds(EntityTypes.IRON_GOLEM, vanillaBounds);
                helper.assertTrue(Math.abs(configuredBounds.getYsize() - vanillaBounds.getYsize() * scale) < 1e-5,
                        "Existing sizing owner must supply the configured adult bounds");
                var checked = SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED,
                        level, target, 1, 0, 0, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, true);
                helper.assertTrue(checked.isPresent() == (scale != 1.45),
                        "Existing type collision check must use adult rule " + scale);
                checked.ifPresent(net.minecraft.world.entity.Entity::discard);
                if (scale == 1.45) {
                    var unchecked = SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED,
                            level, target, 1, 0, 0, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, false);
                    helper.assertTrue(unchecked.isPresent(), "Disabled vanilla collision check must stay disabled");
                    unchecked.ifPresent(net.minecraft.world.entity.Entity::discard);
                }
            }
            reload(helper, Map.of());
            room(helper, target, 3);
            var absent = SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED,
                    level, target, 1, 0, 0, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, true);
            helper.assertTrue(absent.isPresent(), "Absent rule must preserve vanilla adult check");
            absent.ifPresent(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        } finally {
            Files.write(file, original);
            executeReload(helper);
        }
    }

    static void room(GameTestHelper helper, BlockPos target, int height) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                helper.getLevel().setBlockAndUpdate(target.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 6; y++) {
                    helper.getLevel().setBlockAndUpdate(target.offset(x, y, z),
                            (y == height ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
            }
        }
    }

    static void reload(GameTestHelper helper, Map<String, Double> scales) throws Exception {
        var config = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, true, scales);
        Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("realsize.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(config));
        executeReload(helper);
    }

    static void executeReload(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", server.createCommandSourceStack()) == 1,
                "Fixture config reload must succeed");
    }
}
