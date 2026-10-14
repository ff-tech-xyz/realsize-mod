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
    private static int completedResourceReloads;
    private static boolean resourceReloadOnServerThread;

    @GameTest(setupTicks = 1250, maxTicks = 2400)
    public void realDatapackReloadReconcilesTagsAndPreservesFailedSnapshot(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("realsize-reload-test");
        var tags = pack.resolve("data/realsize-test/tags/entity_type");
        Files.createDirectories(tags);
        var format = net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA);
        String version = "[" + format.major() + "," + format.minor() + "]";
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"RealSize reload test\",\"min_format\":"
                + version + ",\"max_format\":" + version + "}}");
        writeTag(tags, "only_bee", "minecraft:cow");
        writeTag(tags, "empty", null);
        writeReloadConfig(Map.of("#realsize-test:only_bee", 0.4, "#realsize-test:empty", 0.8));
        executeReload(server);
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 1);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPersistenceRequired();
        helper.assertTrue(Math.abs(bee.getScale() - 0.4) < 0.00001 && cow.getScale() == 1.0f,
                "Active snapshot must initially use original pack tags");
        byte[] invalidDisk = "not committed config".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, invalidDisk);
        var selected = new java.util.ArrayList<>(server.getPackRepository().getSelectedIds());
        var withFixture = new java.util.ArrayList<>(selected);
        withFixture.add("file/realsize-reload-test");
        server.getPackRepository().reload();
        helper.assertTrue(server.getPackRepository().isAvailable("file/realsize-reload-test"), "Real datapack must be discovered");
        var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
        int firstReload = completedResourceReloads + 1;
        future.set(server.reloadResources(withFixture));
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(future.get().isDone() && completedResourceReloads >= firstReload, "Real resource reload must finish");
        }).thenExecute(() -> {
            future.get().join();
            helper.assertTrue(resourceReloadOnServerThread, "Actual Fabric completion must run on server thread");
            helper.assertTrue(bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null
                    && Math.abs(cow.getScale() - 0.4) < 0.00001, "Changed tags must immediately remove/add loaded sizing");
            assertBytes(helper, file, invalidDisk);
            writeTagUnchecked(tags, "empty", "minecraft:cow");
            future.set(server.reloadResources(withFixture));
        }).thenWaitUntil(() -> {
            helper.assertTrue(future.get().isDone() && completedResourceReloads >= firstReload + 1, "Conflicting pack must finish reload");
        }).thenExecute(() -> {
            future.get().join();
            helper.assertTrue(Math.abs(cow.getScale() - 0.4) < 0.00001 && bee.getScale() == 1.0f,
                    "Tag conflict must keep prior type membership and loaded mobs");
            var newCow = helper.spawn(EntityTypes.COW, 2, 2, 2);
            helper.assertTrue(Math.abs(newCow.getScale() - 0.4) < 0.00001, "Failed tag resolution must preserve snapshot on load");
            assertBytes(helper, file, invalidDisk);
            writeTagUnchecked(tags, "only_bee", "minecraft:bee");
            writeTagUnchecked(tags, "empty", null);
            future.set(server.reloadResources(withFixture));
        }).thenWaitUntil(() -> {
            helper.assertTrue(future.get().isDone() && completedResourceReloads >= firstReload + 2, "Repaired pack must finish reload");
        }).thenExecute(() -> {
            future.get().join();
            helper.assertTrue(Math.abs(bee.getScale() - 0.4) < 0.00001 && cow.getScale() == 1.0f,
                    "Later valid tag reload must recover via same owner");
            assertBytes(helper, file, invalidDisk);
            future.set(server.reloadResources(selected));
        }).thenWaitUntil(() -> {
            helper.assertTrue(future.get().isDone() && completedResourceReloads >= firstReload + 3, "Original packs must be restored");
        }).thenExecute(() -> {
            future.get().join();
            try {
                Files.write(file, original);
                executeReload(server);
                Files.delete(tags.resolve("only_bee.json"));
                Files.delete(tags.resolve("empty.json"));
                Files.delete(pack.resolve("pack.mcmeta"));
                Files.delete(tags);
                Files.delete(tags.getParent());
                Files.delete(tags.getParent().getParent());
                Files.delete(pack.resolve("data"));
                Files.delete(pack);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }).thenSucceed();
    }

    private static void assertBytes(GameTestHelper helper, java.nio.file.Path file, byte[] expected) {
        try {
            helper.assertTrue(java.util.Arrays.equals(expected, Files.readAllBytes(file)), "Datapack reload must not read/rewrite disk edits");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void writeTag(java.nio.file.Path tags, String name, String member) throws IOException {
        Files.writeString(tags.resolve(name + ".json"), "{\"replace\":true,\"values\":["
                + (member == null ? "" : "\"" + member + "\"") + "]}");
    }

    private static void writeTagUnchecked(java.nio.file.Path tags, String name, String member) {
        try {
            writeTag(tags, name, member);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void executeReload(net.minecraft.server.MinecraftServer server) {
        try {
            if (server.getCommands().getDispatcher().execute("realsize reload", server.createCommandSourceStack()) != 1) {
                throw new IllegalStateException("Fixture reload must succeed");
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @GameTest(setupTicks = 40)
    public void registeredReloadIsPermissionGatedTransactionalAndAuthoritative(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var file = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(file);
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        var baby = helper.spawn(EntityTypes.CHICKEN, 2, 2, 1);
        baby.setBaby(true);
        var messages = new java.util.ArrayList<String>();
        var output = new net.minecraft.commands.CommandSource() {
            public void sendSystemMessage(net.minecraft.network.chat.Component message) { messages.add(message.getString()); }
            public boolean acceptsSuccess() { return true; }
            public boolean acceptsFailure() { return true; }
            public boolean shouldInformAdmins() { return false; }
        };
        var source = server.createCommandSourceStack().withSource(output)
                .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER);
        try {
            writeReloadConfig(Map.of("minecraft:bee", 0.4, "minecraft:chicken", 0.4));
            for (var permissions : List.of(net.minecraft.server.permissions.LevelBasedPermissionSet.ALL,
                    net.minecraft.server.permissions.LevelBasedPermissionSet.MODERATOR)) {
                boolean rejected = false;
                try {
                    server.getCommands().getDispatcher().execute("realsize reload", source.withPermission(permissions));
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
                    rejected = true;
                }
                helper.assertTrue(rejected && bee.getScale() == 0.25f, "Levels 0 and 1 must not execute reload");
            }
            helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", source) == 1,
                    "Permission level 2 must execute registered reload");
            helper.assertTrue(Math.abs(bee.getScale() - 0.4) < 0.00001,
                    "Success must immediately reconcile loaded mobs before command returns");
            var owned = bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE);
            byte[] invalid = "{\"configVersion\":2,\"floor\":0}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(file, invalid);
            messages.clear();
            helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", source) == 0,
                    "Malformed candidate must return command failure");
            helper.assertTrue(java.util.Arrays.equals(invalid, Files.readAllBytes(file)), "Invalid bytes must survive reload");
            helper.assertTrue(bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == owned,
                    "Failure must leave loaded modifier unchanged, including identity");
            var next = helper.spawn(EntityTypes.BEE, 1, 2, 2);
            helper.assertTrue(Math.abs(next.getScale() - 0.4) < 0.00001, "Failure must preserve snapshot for subsequent loads");
            helper.assertTrue(messages.stream().anyMatch(m -> m.contains(file.toString()) && m.contains("floor")),
                    "Failure must tell operator which file/field to fix");
            writeReloadConfig(Map.of("#realsize-test:only_bee", 0.8, "#realsize-test:mixed", 0.9));
            byte[] conflicting = Files.readAllBytes(file);
            helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", source) == 0
                    && bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == owned,
                    "Resolution conflict must preserve previous owner and loaded mobs");
            helper.assertTrue(java.util.Arrays.equals(conflicting, Files.readAllBytes(file)), "Conflict bytes must survive");
            byte[] legacyConflict = """
                    {"floor":0.22,"cap":1.45,"entityScales":{
                    "#realsize-test:only_bee":0.8,"#realsize-test:mixed":0.9}}
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(file, legacyConflict);
            helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", source) == 0
                    && bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == owned,
                    "Legacy candidate must resolve before migration or owner mutation");
            helper.assertTrue(java.util.Arrays.equals(legacyConflict, Files.readAllBytes(file))
                    && !Files.exists(file.resolveSibling("realsize.json.legacy.bak")),
                    "Rejected legacy resolution must not rewrite or create a backup");
            writeReloadConfig(Map.of("minecraft:chicken", 1.0));
            helper.assertTrue(server.getCommands().getDispatcher().execute("realsize reload", source) == 1,
                    "Neutral/deleted replacement must succeed");
            helper.assertTrue(bee.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null && bee.getScale() == 1.0f,
                    "Deleted bee rule must remove scale without restoring defaults");
            helper.assertTrue(baby.getAttribute(Attributes.SCALE).getModifier(OWNED_SCALE) == null
                    && baby.getScale() * baby.getAgeScale() == 0.5f, "Neutral rule must preserve vanilla baby size");
        } finally {
            Files.write(file, original);
            server.getCommands().getDispatcher().execute("realsize reload", source);
        }
        helper.succeed();
    }

    private static void writeReloadConfig(Map<String, Double> rules) throws IOException {
        var config = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, rules);
        Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("realsize.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(config) + "\n");
    }

    @GameTest(maxTicks = 1200)
    public void realChunkUnloadReloadMigratesAndDoesNotSerializeTransientScale(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        var configFile = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] originalConfig = Files.readAllBytes(configFile);
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
            try {
                writeReloadConfig(Map.of("minecraft:chicken", 0.8));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            executeReload(level.getServer());
            helper.assertTrue(level.getEntityInAnyDimension(chicken.getUUID()) == null,
                    "Config reload must not load an unloaded fixture");
            level.setChunkForced(chunk.x(), chunk.z(), true);
            level.getChunk(chunk.x(), chunk.z());
        }).thenWaitUntil(() -> {
            var reloaded = level.getEntityInAnyDimension(chicken.getUUID());
            helper.assertTrue(reloaded instanceof Chicken && reloaded != chicken, "Disk load must create a new actual chicken");
        }).thenExecute(() -> {
            var reloaded = (Chicken) level.getEntityInAnyDimension(chicken.getUUID());
            helper.assertTrue(reloaded.isBaby(), "Saved baby age must survive chunk load");
            helper.assertTrue(Math.abs(reloaded.getScale() * reloaded.getAgeScale() - 0.48) < 0.00001,
                    "Disk-loaded baby must use successfully reloaded config and preserve foreign scale");
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
            try {
                Files.write(configFile, originalConfig);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            executeReload(level.getServer());
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
        // GameTest reuses its world. Remove only our fixture after an aborted prior run.
        var stalePack = FabricLoader.getInstance().getGameDir().resolve("world/datapacks/realsize-reload-test");
        if (Files.exists(stalePack)) {
            try (var paths = Files.walk(stalePack)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            completedResourceReloads++;
            resourceReloadOnServerThread = server.isSameThread();
        });
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
