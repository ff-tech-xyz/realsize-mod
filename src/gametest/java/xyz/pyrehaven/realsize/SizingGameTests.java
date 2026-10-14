package xyz.pyrehaven.realsize;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import xyz.pyrehaven.realsize.config.RealSizeConfig;
import xyz.pyrehaven.realsize.config.ResolvedSettings;
import xyz.pyrehaven.realsize.sizing.EntitySizing;

public final class SizingGameTests implements ModInitializer {
    private static final Identifier OWNED_SCALE = Identifier.parse("realsize:scale");
    private static final Identifier FOREIGN_SCALE = Identifier.parse("othermod:scale");

    @GameTest(maxTicks = 1200)
    public void realChunkUnloadReloadMigratesAndDoesNotSerializeTransientScale(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(1024, 5, 1024));
        var chunk = ChunkPos.containing(position);
        level.setChunkForced(chunk.x(), chunk.z(), true);
        level.getChunk(chunk.x(), chunk.z());
        var chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        chicken.setBaby(true);
        chicken.setNoAi(true);
        chicken.setNoGravity(true);
        chicken.setPersistenceRequired();
        chicken.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        var foreign = modifier(FOREIGN_SCALE, 0.2);
        chicken.getAttribute(Attributes.SCALE).addPermanentModifier(foreign);
        helper.assertTrue(level.addFreshEntity(chicken), "Chunk fixture must enter actual world");
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(level.areEntitiesLoaded(chunk.pack()) && level.areEntitiesActuallyLoadedAndTicking(chunk),
                    "Chunk entities must finish loading and tracking before legacy injection");
        }).thenExecute(() -> {
            // Wait until the forced chunk is tracking, then save genuine legacy data.
            chicken.getAttribute(Attributes.SCALE).addPermanentModifier(modifier(Identifier.parse("realsize:old_scale"), 0.8));
            chicken.getAttribute(Attributes.STEP_HEIGHT).addPermanentModifier(modifier(Identifier.parse("realsize:old_step"), 0.5));
            level.save(null, true, false);
            var saved = readChunkEntity(helper, chunk, chicken.getUUID());
            var packed = savedAttributes(helper, saved);
            helper.assertTrue(packed.stream().flatMap(a -> a.modifiers().stream()).noneMatch(m -> m.id().equals(OWNED_SCALE)),
                    "Real on-disk entity must not contain transient owned scale");
            helper.assertTrue(packed.stream().flatMap(a -> a.modifiers().stream())
                    .anyMatch(m -> m.id().equals(Identifier.parse("realsize:old_scale"))), "Fixture must really save legacy data");
            writeEvidence(helper, "chunk-legacy", saved);
            level.setChunkForced(chunk.x(), chunk.z(), false);
        }).thenWaitUntil(() -> {
            helper.assertTrue(chicken.isRemoved() && chicken.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK,
                    "Chunk manager must really unload, not discard or simulate reload");
            helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null, "Actual chunk must unload");
        }).thenExecute(() -> {
            level.setChunkForced(chunk.x(), chunk.z(), true);
            level.getChunk(chunk.x(), chunk.z());
        }).thenWaitUntil(() -> {
            var reloaded = level.getEntityInAnyDimension(chicken.getUUID());
            helper.assertTrue(reloaded instanceof Chicken && reloaded != chicken, "Disk load must create a new actual chicken");
        }).thenExecute(() -> {
            var reloaded = (Chicken) level.getEntityInAnyDimension(chicken.getUUID());
            helper.assertTrue(reloaded.isBaby(), "Saved baby age must survive chunk load");
            helper.assertTrue(Math.abs(reloaded.getScale() * reloaded.getAgeScale() - 0.264) < 0.00001,
                    "Registered load owner must use current policy and preserve foreign scale");
            helper.assertTrue(reloaded.getAttribute(Attributes.SCALE).getPermanentModifiers().equals(java.util.Set.of(foreign)),
                    "Load must remove legacy scale and preserve foreign data");
            helper.assertTrue(reloaded.getAttribute(Attributes.STEP_HEIGHT).getModifiers().isEmpty(),
                    "Load must remove old movement without implementing new movement");
            level.save(null, true, false);
            var saved = readChunkEntity(helper, chunk, chicken.getUUID());
            var packed = savedAttributes(helper, saved);
            helper.assertTrue(packed.stream().flatMap(a -> a.modifiers().stream())
                    .noneMatch(m -> m.id().getNamespace().equals("realsize")), "Migrated disk entity must contain no owned modifiers");
            helper.assertTrue(packed.stream().flatMap(a -> a.modifiers().stream()).anyMatch(foreign::equals),
                    "Foreign scale must remain on disk after migration");
            writeEvidence(helper, "chunk-migrated", saved);
            level.setChunkForced(chunk.x(), chunk.z(), false);
        }).thenSucceed();
    }

    private static List<AttributeInstance.Packed> savedAttributes(GameTestHelper helper, CompoundTag saved) {
        return TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved)
                .read("attributes", AttributeInstance.Packed.LIST_CODEC).orElseThrow();
    }

    private static CompoundTag readChunkEntity(GameTestHelper helper, ChunkPos chunk, java.util.UUID uuid) {
        var level = helper.getLevel();
        var directory = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
                level.getServer().getWorldPath(LevelResource.ROOT)).resolve("entities");
        var file = directory.resolve("r." + chunk.getRegionX() + "." + chunk.getRegionZ() + ".mca");
        helper.assertTrue(Files.isRegularFile(file), "Real entity region file must exist");
        try (var region = new RegionFile(new RegionStorageInfo("GameTest", level.dimension(), "entities"), file, directory, false);
                var input = region.getChunkDataInputStream(chunk)) {
            helper.assertTrue(input != null, "Actual chunk must have persisted entity NBT");
            var savedChunk = NbtIo.read(input);
            for (var tag : savedChunk.getListOrEmpty("Entities")) {
                var entity = tag.asCompound().orElseThrow();
                var inputEntity = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entity);
                if (inputEntity.read("UUID", net.minecraft.core.UUIDUtil.CODEC).orElseThrow().equals(uuid)) {
                    return entity;
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        throw helper.assertionException("Persisted chunk must contain the fixture UUID");
    }

    private static void writeEvidence(GameTestHelper helper, String name, CompoundTag saved) {
        var directory = FabricLoader.getInstance().getGameDir().resolve("sizing-evidence");
        try {
            Files.createDirectories(directory);
            NbtIo.writeCompressed(saved, directory.resolve(name + ".nbt"));
            Files.writeString(directory.resolve(name + ".snbt"), saved.toString() + "\n");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void onInitialize() {
        // Only this development mod writes the disposable GameTest server fixture.
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        var fixture = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true,
                Map.of("minecraft:bee", 0.25, "minecraft:chicken", 0.25,
                        "minecraft:zombie", 0.1, "minecraft:cow", 1.0));
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(fixture) + "\n");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @GameTest(maxTicks = 100)
    public void registeredLifecycleLoadsAndReconcilesNaturalGrowth(GameTestHelper helper) {
        var chicken = EntityTypes.CHICKEN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        chicken.setAge(-2);
        chicken.setPersistenceRequired();
        chicken.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        helper.assertTrue(helper.getLevel().addFreshEntity(chicken), "Actual baby must join server level");
        helper.assertTrue(Math.abs(chicken.getScale() * chicken.getAgeScale() - 0.22) < 0.00001,
                "Registered entity-load callback must bound actual baby chicken");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!chicken.isBaby(), "Vanilla ticking must grow the baby");
            helper.assertTrue(Math.abs(chicken.getScale() - 0.25) < 0.00001,
                    "Registered tick reconciliation must update through same owner after natural growth");
            helper.assertTrue(Math.abs(chicken.getBoundingBox().getXsize()
                    - EntityTypes.CHICKEN.getDimensions().width() * 0.25) < 0.00001,
                    "Vanilla attribute updates must refresh actual collision dimensions after growth");
            var zombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 2);
            zombie.setBaby(true);
            var cow = helper.spawn(EntityTypes.COW, 1, 2, 2);
            cow.setBaby(true);
            var pig = helper.spawn(EntityTypes.PIG, 2, 2, 1);
            pig.setBaby(true);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(Math.abs(zombie.getScale() * zombie.getAgeScale() - 0.6) < 0.00001,
                        "Hostile actual baby must reach category floor");
                helper.assertTrue(cow.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null
                        && cow.getScale() * cow.getAgeScale() == 0.5f, "Neutral baby must remain vanilla");
                helper.assertTrue(pig.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null
                        && pig.getScale() * pig.getAgeScale() == 0.5f, "Absent baby must remain vanilla");
                helper.succeed();
            });
        });
    }

    @GameTest
    public void loadStripsOnlyLegacyPermanentOwnedAttributes(GameTestHelper helper) {
        var bee = EntityTypes.BEE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        var scale = bee.getAttribute(Attributes.SCALE);
        var health = bee.getAttribute(Attributes.MAX_HEALTH);
        var step = bee.getAttribute(Attributes.STEP_HEIGHT);
        double scaleBase = scale.getBaseValue();
        double healthBase = health.getBaseValue();
        double stepBase = step.getBaseValue();
        var foreign = modifier(FOREIGN_SCALE, 0.2);
        scale.addPermanentModifier(modifier(Identifier.parse("realsize:old_scale"), 0.8));
        scale.addPermanentModifier(foreign);
        health.addPermanentModifier(modifier(Identifier.parse("realsize:unknown_old_id"), 0.5));
        step.addPermanentModifier(modifier(Identifier.parse("realsize:step_height"), 0.5));
        var transientOther = modifier(Identifier.parse("realsize:unrelated_transient"), 0.1);
        health.addTransientModifier(transientOther);
        var similarNamespace = modifier(Identifier.parse("realsize_extra:scale"), 0.1);
        health.addPermanentModifier(similarNamespace);
        owner(helper, Map.of("minecraft:bee", 0.25)).onLoad(bee);
        helper.assertTrue(scale.getPermanentModifiers().equals(java.util.Set.of(foreign)),
                "Remove persistent realsize scale, preserve foreign scale");
        helper.assertTrue(health.getModifiers().equals(java.util.Set.of(transientOther, similarNamespace)),
                "Cleanup must include all attributes but only exact namespace permanent modifiers");
        helper.assertTrue(step.getModifiers().isEmpty(), "Legacy step data must be removed without new movement");
        helper.assertTrue(scale.getBaseValue() == scaleBase && health.getBaseValue() == healthBase
                && step.getBaseValue() == stepBase, "Base attributes must remain unchanged");
        helper.assertTrue(Math.abs(bee.getScale() - 0.3) < 0.00001, "Foreign multiplier must coexist");
        helper.succeed();
    }

    @GameTest
    public void neutralDeletedRulesAndSavedDataPreserveVanillaBabies(GameTestHelper helper) {
        var chicken = EntityTypes.CHICKEN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        chicken.setBaby(true);
        var attribute = chicken.getAttribute(Attributes.SCALE);
        var foreign = modifier(FOREIGN_SCALE, 0.2);
        attribute.addPermanentModifier(foreign);
        owner(helper, Map.of("minecraft:chicken", 0.25)).onLoad(chicken);
        helper.assertTrue(Math.abs(chicken.getScale() * chicken.getAgeScale() - 0.264) < 0.00001,
                "Active baby must bound final adult-relative scale before foreign effects");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        helper.assertTrue(chicken.save(output), "Actual entity must serialize");
        var saved = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult())
                .read("attributes", AttributeInstance.Packed.LIST_CODEC).orElseThrow();
        helper.assertTrue(saved.stream().flatMap(a -> a.modifiers().stream()).noneMatch(m -> m.id().equals(OWNED_SCALE)),
                "Transient RealSize scale must not serialize");
        helper.assertTrue(saved.stream().flatMap(a -> a.modifiers().stream()).anyMatch(foreign::equals),
                "Persistent foreign scale must serialize unchanged");
        owner(helper, Map.of("minecraft:chicken", 1.0)).reconcile(chicken);
        helper.assertTrue(attribute.getModifier(OWNED_SCALE) == null
                && Math.abs(chicken.getScale() * chicken.getAgeScale() - 0.6) < 0.00001,
                "Neutral must preserve vanilla baby size and foreign scale, not category floor");
        owner(helper, Map.of("minecraft:chicken", 0.25)).reconcile(chicken);
        owner(helper, Map.of()).reconcile(chicken);
        helper.assertTrue(attribute.getModifiers().equals(java.util.Set.of(foreign)),
                "Deleted rule must remove only stable owned modifier");
        helper.succeed();
    }

    private static AttributeModifier modifier(Identifier id, double amount) {
        return new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @GameTest
    public void excludedEntitiesStayNeutralAndForeignBoundsAreDiagnosed(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.CREATIVE);
        player.getAttribute(Attributes.SCALE).addPermanentModifier(modifier(OWNED_SCALE, 0.5));
        var owner = owner(helper, Map.of("minecraft:player", 0.3, "minecraft:arrow", 1.4));
        owner.onLoad(player);
        helper.assertTrue(player.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null,
                "Excluded player must lose legacy owned scale and remain unscaled");
        owner.onLoad(EntityTypes.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND));
        var bee = EntityTypes.BEE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        var foreign = modifier(FOREIGN_SCALE, 9.0);
        bee.getAttribute(Attributes.SCALE).addPermanentModifier(foreign);
        var diagnostics = new java.util.ArrayList<String>();
        var config = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, Map.of("minecraft:bee", 0.25));
        var diagnosed = new EntitySizing(ResolvedSettings.resolve(config,
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), diagnostics::add), diagnostics::add);
        diagnosed.onLoad(bee);
        diagnosed.reconcile(bee);
        helper.assertTrue(bee.getAttribute(Attributes.SCALE).getPermanentModifiers().equals(java.util.Set.of(foreign)),
                "Bounds diagnostic must not remove foreign modifiers");
        helper.assertTrue(diagnostics.size() == 1 && diagnostics.getFirst().contains(bee.getStringUUID()),
                "Foreign bounds violation must name actual mob without warning each unchanged tick");
        helper.succeed();
    }

    @GameTest
    public void activeScaleIsTransientAndIdempotent(GameTestHelper helper) {
        var bee = EntityTypes.BEE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        var owner = owner(helper, Map.of("minecraft:bee", 0.25));
        owner.onLoad(bee);
        helper.assertTrue(Math.abs(bee.getScale() - 0.25) < 0.00001, "Actual bee must use current scale");
        var modifier = bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE);
        owner.onLoad(bee);
        owner.reconcile(bee);
        helper.assertTrue(Math.abs(bee.getScale() - 0.25) < 0.00001, "Repeated load/reconciliation must not stack");
        helper.assertTrue(bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == modifier,
                "Unchanged reconciliation must not replace or dirty the modifier");
        owner(helper, Map.of("minecraft:bee", 0.4)).reconcile(bee);
        helper.assertTrue(Math.abs(bee.getScale() - 0.4) < 0.00001
                && bee.getAttribute(Attributes.SCALE).getModifiers().size() == 1,
                "A replacement snapshot must update the same stable ID, not stack");
        helper.succeed();
    }

    private static EntitySizing owner(GameTestHelper helper, Map<String, Double> rules) {
        var config = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, rules);
        return new EntitySizing(ResolvedSettings.resolve(config,
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), message -> {}),
                message -> {});
    }
}
