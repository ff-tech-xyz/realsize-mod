package xyz.pyrehaven.realsize;

import java.nio.file.Files;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SpawnDimensionGameTests {
    @GameTest(setupTicks = 6300)
    public void commandsExistingMobsAndReloadNeverUsePlacementVeto(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var target = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnGameTests.room(helper, target, 3);
        var mobs = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        try {
            SpawnGameTests.reload(helper, Map.of());
            var existing = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
            existing.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            existing.setNoAi(true);
            existing.setNoGravity(true);
            level.addFreshEntity(existing);
            mobs.add(existing);
            var expected = existing.position();
            var vanilla = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
            vanilla.snapTo(expected);
            vanilla.setNoAi(true);
            vanilla.setNoGravity(true);
            vanilla.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(
                    Identifier.parse("spawn-test:vanilla-growth"), 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            vanilla.tick();
            SpawnGameTests.reload(helper, Map.of("minecraft:iron_golem", 1.45));
            helper.assertTrue(existing.position().equals(expected) && !existing.isRemoved(), "Reload itself must not relocate or remove existing mobs");
            existing.tick();
            helper.assertTrue(!level.noCollision(existing, existing.getBoundingBox()) && !existing.isRemoved()
                    && existing.position().equals(vanilla.position()), "Collision must not add relocation beyond vanilla attribute refresh");
            vanilla.discard();
            SpawnGameTests.registered.clear();
            var server = level.getServer();
            String summon = String.format(java.util.Locale.ROOT, "summon minecraft:iron_golem %.1f %d %.1f {NoAI:1b,NoGravity:1b}",
                    target.getX() + 2.5, target.getY(), target.getZ() + 0.5);
            helper.assertTrue(server.getCommands().getDispatcher().execute(summon, server.createCommandSourceStack()) == 1,
                    "Actual summon must succeed in colliding space");
            var summoned = SpawnGameTests.registered.stream().filter(e -> e.getType() == EntityTypes.IRON_GOLEM).findFirst().orElseThrow();
            mobs.add(summoned);
            var summonedPosition = summoned.position();
            var summonControl = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
            summonControl.snapTo(summonedPosition);
            summonControl.setNoAi(true);
            summonControl.setNoGravity(true);
            summonControl.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(
                    Identifier.parse("spawn-test:vanilla-growth"), 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            summonControl.tick();
            ((net.minecraft.world.entity.Mob) summoned).tick();
            helper.assertTrue(!summoned.isRemoved() && summoned.position().equals(summonControl.position())
                    && !level.noCollision(summoned, summoned.getBoundingBox()), "Explicit summons must not be collision-vetoed or relocated");
            summonControl.discard();
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
            helper.assertTrue(summoned.save(output), "Actual entity must save");
            var loaded = net.minecraft.world.entity.EntityType.loadEntityRecursive(EntityTypes.IRON_GOLEM, output.buildResult(),
                    level, EntitySpawnReason.LOAD, entity -> entity);
            loaded.setUUID(java.util.UUID.randomUUID());
            level.addFreshEntity(loaded);
            mobs.add(loaded);
            helper.assertTrue(!loaded.isRemoved() && loaded.position().equals(summoned.position()),
                    "Saved entity load must bypass placement scope without moving or removing it");
            helper.succeed();
        } finally {
            mobs.forEach(net.minecraft.world.entity.Entity::discard);
            Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }

    @GameTest(setupTicks = 4800)
    public void growthAttributesRefreshWithinTheEntityTick(GameTestHelper helper) throws Exception {
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var chicken = EntityTypes.CHICKEN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        chicken.setAge(-1);
        chicken.setNoAi(true);
        chicken.setNoGravity(true);
        chicken.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 2, 4)));
        try {
            SpawnGameTests.reload(helper, Map.of("minecraft:chicken", 0.25));
            helper.assertTrue(helper.getLevel().addFreshEntity(chicken), "Lifecycle fixture must register");
            chicken.tick();
            helper.assertTrue(!chicken.isBaby(), "One actual entity tick must grow age -1");
            helper.assertTrue(Math.abs(chicken.getScale() - 0.25) < 1e-6, "Growth callback must update scale within the entity tick");
            helper.assertTrue(chicken.getAttributes().getAttributesToUpdate().isEmpty(),
                    "The same entity tick consumes growth attribute dirt; no end-level repair is needed");
            helper.assertTrue(Math.abs(chicken.getBoundingBox().getXsize() - EntityTypes.CHICKEN.getDimensions().width() * 0.25) < 1e-6,
                    "The growth tick refreshes adult collision dimensions deterministically");
            helper.succeed();
        } finally {
            chicken.discard();
            Files.write(file, original);
            SpawnGameTests.executeReload(helper);
        }
    }

}
