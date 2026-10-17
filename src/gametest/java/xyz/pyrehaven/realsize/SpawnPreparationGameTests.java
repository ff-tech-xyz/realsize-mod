package xyz.pyrehaven.realsize;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

public final class SpawnPreparationGameTests {
    @GameTest(setupTicks = 5400, maxTicks = 400)
    public void acceptedExistingChickenMatchesActualVanillaFinalization(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnGameTests.room(helper, target, 6);
        // SpawnUtil also checks dark walk-target admission; open-sided roofs do not provide it.
        for (int y = 0; y < 6; y++) {
            for (int offset = -3; offset <= 3; offset++) {
                for (var wall : java.util.List.of(target.offset(-3, y, offset), target.offset(3, y, offset),
                        target.offset(offset, y, -3), target.offset(offset, y, 3))) {
                    level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                }
            }
        }
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(level.getMaxLocalRawBrightness(target) == 0, "Dark-room lighting must finish before vanilla admission");
        }).thenExecute(() -> {
            try {
                checkExistingChicken(helper, target);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).thenSucceed();
    }

    private static void checkExistingChicken(GameTestHelper helper, BlockPos target) throws Exception {
        var level = helper.getLevel();
        var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = java.nio.file.Files.readAllBytes(file);
        var chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        chicken.snapTo(target.getX() + 1.5, target.getY(), target.getZ() + 0.5);
        chicken.setNoAi(true);
        chicken.setNoGravity(true);
        chicken.setPersistenceRequired();
        level.addFreshEntity(chicken);
        var chickenPosition = chicken.position();
        var chickenBox = chicken.getBoundingBox();
        int mounted = 0;
        try {
            SpawnGameTests.reload(helper, java.util.Map.of());
            for (int seed = 12000; seed < 13000; seed++) {
                NaturalSpawnProbe.reset();
                final int entitySeed = seed;
                NaturalSpawnProbe.constructedHook = mob -> mob.getRandom().setSeed(entitySeed);
                level.getRandom().setSeed(seed);
                level.getRandom().nextInt(1);
                level.getRandom().nextInt(1);
                SpawnGameTests.registered.clear();
                var control = EntityTypes.ZOMBIE.create(level, null, target, EntitySpawnReason.MOB_SUMMONED, false, false);
                if (control.getVehicle() != chicken) {
                    tree(control).forEach(net.minecraft.world.entity.Entity::discard);
                    SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
                    continue;
                }
                chicken.positionRider(control);
                helper.assertTrue(control.checkSpawnRules(level, EntitySpawnReason.MOB_SUMMONED)
                        && control.checkSpawnObstruction(level), "Vanilla control must satisfy actual admission rules");
                level.addFreshEntityWithPassengers(control);
                var expectedPosition = control.position();
                var expectedTypes = SpawnGameTests.registered.stream().map(net.minecraft.world.entity.Entity::getType).toList();
                int events = NaturalSpawnProbe.mountEvents;
                control.discard();
                chicken.setChickenJockey(false);
                NaturalSpawnProbe.reset();
                NaturalSpawnProbe.constructedHook = mob -> mob.getRandom().setSeed(entitySeed);
                level.getRandom().setSeed(seed);
                SpawnGameTests.registered.clear();
                var attempt = net.minecraft.util.SpawnUtil.trySpawnMob(EntityTypes.ZOMBIE, EntitySpawnReason.MOB_SUMMONED,
                        level, target, 1, 0, 0, net.minecraft.util.SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false);
                helper.assertTrue(attempt.isPresent(), "Vanilla-accepted existing-chicken seed rejected: " + seed
                        + " candidates=" + NaturalSpawnProbe.created.stream().map(e -> e.position() + ":" + e.getBoundingBox()
                        + ":rules=" + e.checkSpawnRules(level, EntitySpawnReason.MOB_SUMMONED) + ":obstruction=" + e.checkSpawnObstruction(level)).toList());
                var accepted = attempt.orElseThrow();
                helper.assertTrue(accepted.getVehicle() == chicken && chicken.isChickenJockey(), "Existing-chicken choice must survive acceptance");
                helper.assertTrue(accepted.position().equals(expectedPosition) && chicken.position().equals(chickenPosition)
                        && chicken.getBoundingBox().equals(chickenBox), "Clearance must match actual attachment without moving/resizing existing chicken");
                helper.assertTrue(NaturalSpawnProbe.mountEvents == events
                        && SpawnGameTests.registered.stream().map(net.minecraft.world.entity.Entity::getType).toList().equals(expectedTypes),
                        "Accepted events and registration order must match vanilla");
                accepted.discard();
                chicken.setChickenJockey(false);
                mounted++;
            }
            helper.assertTrue(mounted > 0, "Seed range must include an actual existing-chicken jockey");
        } finally {
            NaturalSpawnProbe.reset();
            chicken.discard();
            java.nio.file.Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }

    @GameTest(setupTicks = 6000)
    public void actualHuskChickenToCamelSwitchKeepsAcceptedGeometry(GameTestHelper helper) throws Exception {
        var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = java.nio.file.Files.readAllBytes(file);
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnGameTests.room(helper, target, 6);
        var rider = EntityTypes.HUSK.create(level, EntitySpawnReason.NATURAL);
        rider.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        level.getRandom().setSeed(58);
        rider.getRandom().setSeed(58);
        try {
            SpawnGameTests.reload(helper, java.util.Map.of());
            SpawnGameTests.registered.clear();
            SpawnPreparation.begin(rider);
            rider.finalizeSpawn(level, level.getCurrentDifficultyAt(target), EntitySpawnReason.NATURAL,
                    new net.minecraft.world.entity.monster.zombie.Zombie.ZombieGroupData(true, true));
            helper.assertTrue(rider.getVehicle() != null && rider.getVehicle().getType() == EntityTypes.CAMEL_HUSK,
                    "Pinned vanilla seed must exercise Husk's forced camel switch");
            helper.assertTrue(SpawnPreparation.current().clear(level), "Open room must admit actual vanilla companion tree");
            var checkedPosition = rider.position();
            var checkedBox = rider.getBoundingBox();
            SpawnPreparation.current().commit();
            helper.assertTrue(rider.position().equals(checkedPosition) && rider.getBoundingBox().equals(checkedBox),
                    "Implicit remount must not move accepted geometry after clearance: checked=" + checkedPosition + " actual=" + rider.position());
            rider.getVehicle().positionRider(rider);
            helper.assertTrue(rider.position().equals(checkedPosition), "First vanilla riding tick must agree with accepted clearance");
            helper.succeed();
        } finally {
            SpawnPreparation.closeTo(null);
            tree(rider).forEach(net.minecraft.world.entity.Entity::discard);
            SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
            SpawnGameTests.registered.clear();
            java.nio.file.Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }

    @GameTest(setupTicks = 4500)
    public void acceptedDismountAndRemountMatchVanilla(GameTestHelper helper) {
        var level = helper.getLevel();
        for (boolean accepted : java.util.List.of(false, true)) {
            var first = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            var second = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            var rider = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
            NaturalSpawnProbe.reset();
            SpawnPreparation.begin(rider);
            try {
                helper.assertTrue(rider.startRiding(first, false, true), "First projected mount must succeed");
                rider.removeVehicle();
                helper.assertTrue(!rider.isPassenger() && first.getPassengers().isEmpty(), "Dismount must update projected membership");
                helper.assertTrue(!rider.startRiding(second, false, true), "Vanilla dismount cooldown must prevent nonforced remount");
                helper.assertTrue(rider.startRiding(second, true, true), "Forced remount must succeed");
                helper.assertTrue(NaturalSpawnProbe.mountEvents == 0 && NaturalSpawnProbe.dismountEvents == 0,
                        "Preparation must contain all mount/dismount events");
                if (accepted) SpawnPreparation.current().commit();
                else SpawnPreparation.current().abort();
                helper.assertTrue(first.getPassengers().isEmpty(), "Old vehicle must remain empty");
                helper.assertTrue(second.hasPassenger(rider) == accepted, "Only acceptance commits final mount");
                helper.assertTrue(NaturalSpawnProbe.mountEvents == (accepted ? 2 : 0)
                        && NaturalSpawnProbe.dismountEvents == (accepted ? 1 : 0), "Replay must emit exactly vanilla events");
            } finally {
                SpawnPreparation.closeTo(null);
                rider.discard();
                first.discard();
                second.discard();
                NaturalSpawnProbe.reset();
            }
        }
        helper.succeed();
    }

    @GameTest(setupTicks = 3900)
    public void acceptedMountPlansMatchVanillaAndRejectedPlansEmitNoEvents(GameTestHelper helper) throws Exception {
        var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = java.nio.file.Files.readAllBytes(file);
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnGameTests.room(helper, target, 6);
        var coverage = new java.util.LinkedHashMap<String, Integer>();
        try {
            SpawnGameTests.reload(helper, java.util.Map.of());
            for (var type : java.util.List.of(EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.SPIDER, EntityTypes.STRIDER)) {
                int mounted = 0;
                for (int seed = 60000; seed < 61000; seed++) {
                    level.getRandom().setSeed(seed);
                    var vanilla = type.create(level, EntitySpawnReason.NATURAL);
                    vanilla.getRandom().setSeed(seed);
                    vanilla.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                    SpawnGameTests.registered.clear();
                    NaturalSpawnProbe.reset();
                    vanilla.finalizeSpawn(level, level.getCurrentDifficultyAt(target), EntitySpawnReason.NATURAL, null);
                    var expected = tree(vanilla);
                    // Companion constructors use independently seeded RNGs for yaw.
                    // Compare identical attachment inputs, not unrelated entropy.
                    expected.forEach(e -> e.setYRot(30));
                    for (var entity : expected) {
                        entity.refreshDimensions();
                        if (entity.getVehicle() != null) entity.getVehicle().positionRider(entity);
                    }
                    var signature = signature(expected);
                    int events = NaturalSpawnProbe.mountEvents;
                    var registeredTypes = SpawnGameTests.registered.stream().map(e -> e.getType()).toList();
                    expected.forEach(net.minecraft.world.entity.Entity::discard);
                    SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
                    SpawnGameTests.registered.clear();

                    level.getRandom().setSeed(seed);
                    var candidate = type.create(level, EntitySpawnReason.NATURAL);
                    candidate.getRandom().setSeed(seed);
                    candidate.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                    NaturalSpawnProbe.reset();
                    SpawnPreparation.begin(candidate);
                    try {
                        candidate.finalizeSpawn(level, level.getCurrentDifficultyAt(target), EntitySpawnReason.NATURAL, null);
                        helper.assertTrue(NaturalSpawnProbe.mountEvents == 0 && NaturalSpawnProbe.dismountEvents == 0
                                && SpawnGameTests.registered.isEmpty(), "Preparation must contain mount events and registration");
                        var actual = tree(candidate);
                        actual.forEach(e -> e.setYRot(30));
                        if (actual.size() > 1) mounted++;
                        helper.assertTrue(SpawnPreparation.current().clear(level), "Open room must admit prepared companion tree");
                        helper.assertTrue(signature(actual).equals(signature), "Prepared positions and variants differ from vanilla " + type + " seed " + seed
                                + " expected=" + signature + " actual=" + signature(actual));
                        if (seed % 2 == 0) {
                            SpawnPreparation.current().abort();
                            helper.assertTrue(actual.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved)
                                    && NaturalSpawnProbe.mountEvents == 0 && NaturalSpawnProbe.dismountEvents == 0
                                    && SpawnGameTests.registered.isEmpty(), "Abort must discard all fresh siblings/vehicles without events");
                        } else {
                            SpawnPreparation.current().commit();
                            helper.assertTrue(signature(tree(candidate)).equals(signature), "Accepted replay must preserve attachment positions");
                            helper.assertTrue(NaturalSpawnProbe.mountEvents == events, "Accepted replay must emit vanilla once-only mount events");
                            helper.assertTrue(SpawnGameTests.registered.stream().map(e -> e.getType()).toList().equals(registeredTypes),
                                    "Accepted registration sequence must match vanilla");
                            for (var entity : actual) {
                                var position = entity.position();
                                if (entity.getVehicle() != null) entity.getVehicle().positionRider(entity);
                                helper.assertTrue(entity.position().distanceToSqr(position) < 1e-12, "First actual riding update must agree with clearance");
                            }
                            actual.forEach(net.minecraft.world.entity.Entity::discard);
                            SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
                        }
                    } finally {
                        SpawnPreparation.closeTo(null);
                        SpawnGameTests.registered.clear();
                    }
                }
                helper.assertTrue(mounted > 0, "Seed range must exercise actual companion creation for " + type);
                coverage.put(type.toString(), mounted);
            }
            java.nio.file.Files.writeString(file.getParent().resolve("mount-coverage.txt"), coverage.toString());
            helper.succeed();
        } finally {
            NaturalSpawnProbe.reset();
            java.nio.file.Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }

    private static java.util.List<net.minecraft.world.entity.Entity> tree(net.minecraft.world.entity.Entity root) {
        while (root.getVehicle() != null) root = root.getVehicle();
        var result = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        append(root, result);
        return result;
    }

    private static void append(net.minecraft.world.entity.Entity root, java.util.List<net.minecraft.world.entity.Entity> result) {
        result.add(root);
        root.getPassengers().forEach(child -> append(child, result));
    }

    private static java.util.List<String> signature(java.util.List<net.minecraft.world.entity.Entity> tree) {
        return tree.stream().map(e -> e.getType() + ":" + ((net.minecraft.world.entity.Mob) e).isBaby()
                + ":" + e.position() + ":" + e.getPassengers().size()).toList();
    }

    @GameTest(setupTicks = 3000)
    public void projectedGroupAndMountReadsMatchCommitWithoutMutatingOnAbort(GameTestHelper helper) {
        var level = helper.getLevel();
        var group = new AgeableMob.AgeableMobGroupData(false);
        group.increaseGroupSizeByOne();
        var mob = EntityTypes.MOOSHROOM.create(level, EntitySpawnReason.NATURAL);
        SpawnPreparation.begin(mob);
        try {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, group);
            helper.assertTrue(group.getGroupSize() == 2, "Outer finalizers must see the pending group increment");
        } finally {
            SpawnPreparation.current().abort();
        }
        helper.assertTrue(group.getGroupSize() == 1, "Aborted candidate must leave shared group untouched");

        var strider = EntityTypes.STRIDER.create(level, EntitySpawnReason.NATURAL);
        SpawnPreparation.begin(strider);
        try {
            var first = EntityTypes.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.JOCKEY);
            var second = EntityTypes.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.JOCKEY);
            helper.assertTrue(first.startRiding(strider, false, false), "First rider must be admitted");
            helper.assertTrue(first.isPassenger() && strider.isVehicle(), "Projected membership must match vanilla field reads");
            helper.assertTrue(!second.startRiding(strider, false, false), "Virtual strider capacity must reject second rider");
        } finally {
            SpawnPreparation.current().abort();
        }
        helper.succeed();
    }

    @GameTest(setupTicks = 3300)
    public void fishSchoolEffectCommitsOnlyForAcceptedCandidate(GameTestHelper helper) {
        var level = helper.getLevel();
        var leader = EntityTypes.COD.create(level, EntitySpawnReason.COMMAND);
        var group = new AbstractSchoolingFish.SchoolSpawnGroupData(leader);
        var follower = EntityTypes.COD.create(level, EntitySpawnReason.NATURAL);
        SpawnPreparation.begin(follower);
        follower.finalizeSpawn(level, level.getCurrentDifficultyAt(follower.blockPosition()), EntitySpawnReason.NATURAL, group);
        SpawnPreparation.current().abort();
        helper.assertTrue(!leader.hasFollowers(), "Rejected fish must not increment an existing school leader");
        var accepted = EntityTypes.COD.create(level, EntitySpawnReason.NATURAL);
        SpawnPreparation.begin(accepted);
        accepted.finalizeSpawn(level, level.getCurrentDifficultyAt(accepted.blockPosition()), EntitySpawnReason.NATURAL, group);
        SpawnPreparation.current().commit();
        helper.assertTrue(leader.hasFollowers() && accepted.isFollower(), "Accepted fish must retain its real school attachment");
        accepted.stopFollowing();
        helper.assertTrue(!leader.hasFollowers(), "Exactly one follower increment must commit");
        accepted.discard();
        leader.discard();
        helper.succeed();
    }

    @GameTest(setupTicks = 3600)
    public void rejectedDrownedAndHuskFinalizersDoNotRegisterCompanions(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnGameTests.room(helper, target, 6);
        int[] leaked = {0};
        for (var type : java.util.List.of(EntityTypes.DROWNED, EntityTypes.HUSK)) {
            for (int seed = 50000; seed < 52000; seed++) {
                level.getRandom().setSeed(seed);
                var mob = type.create(level, EntitySpawnReason.NATURAL);
                mob.getRandom().setSeed(seed);
                mob.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                SpawnGameTests.registered.clear();
                SpawnPreparation.begin(mob);
                try {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(target), EntitySpawnReason.NATURAL, null);
                } finally {
                    SpawnPreparation.current().abort();
                }
                leaked[0] += SpawnGameTests.registered.size();
                SpawnGameTests.registered.forEach(net.minecraft.world.entity.Entity::discard);
                SpawnGameTests.registered.clear();
            }
        }
        helper.assertTrue(leaked[0] == 0, "Rejected finalizers registered companion entities: " + leaked[0]);
        helper.succeed();
    }
}
