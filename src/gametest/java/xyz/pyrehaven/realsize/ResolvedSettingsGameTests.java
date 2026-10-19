package xyz.pyrehaven.realsize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import xyz.pyrehaven.realsize.config.ConfigFile;
import xyz.pyrehaven.realsize.config.RealSizeConfig;
import xyz.pyrehaven.realsize.config.ResolvedSettings;

public final class ResolvedSettingsGameTests {
    @GameTest
    public void snapshotsDoNotFollowMutableRulesOrRegistryTagBindings(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        var rules = new HashMap<String, Double>();
        rules.put("#realsize-test:only_bee", 0.8);
        var parsed = config(rules);
        var original = ResolvedSettings.resolve(parsed, registry, message -> {});
        rules.clear();
        rules.put("minecraft:cow", 1.2);
        var originalTags = new HashMap<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>>();
        registry.getTags().forEach(tag -> originalTags.put(tag.key(), tag.stream().toList()));
        var changedTags = new HashMap<>(originalTags);
        changedTags.put(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("realsize-test:only_bee")),
                List.of(registry.wrapAsHolder(EntityTypes.COW)));
        try {
            registry.prepareTagReload(new TagLoader.LoadResult<>(Registries.ENTITY_TYPE, changedTags)).apply();
            var replacement = ResolvedSettings.resolve(parsed, registry, message -> {});
            helper.assertTrue(Double.valueOf(0.8).equals(original.scale(EntityTypes.BEE))
                    && original.scale(EntityTypes.COW) == null, "Existing snapshot must not follow tag rebinding");
            helper.assertTrue(Double.valueOf(0.8).equals(replacement.scale(EntityTypes.COW))
                    && replacement.scale(EntityTypes.BEE) == null, "New resolution must observe current tag binding");
            helper.assertTrue(parsed.entityScales().equals(Map.of("#realsize-test:only_bee", 0.8)),
                    "Snapshot config must not follow mutable input map");
            var empty = ResolvedSettings.resolve(config(Map.of()), registry, message -> {});
            helper.assertTrue(empty.scale(EntityTypes.BEE) == null && empty.scale(EntityTypes.COW) == null,
                    "Empty replacement must not merge old or default rules");
        } finally {
            registry.prepareTagReload(new TagLoader.LoadResult<>(Registries.ENTITY_TYPE, originalTags)).apply();
        }
        helper.succeed();
    }

    @GameTest
    public void equalTagsAgreeAndExactRulesOverrideAmbiguousTags(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        var equal = ResolvedSettings.resolve(config(Map.of(
                "#realsize-test:only_bee", 0.8, "#realsize-test:mixed", 0.8)), registry, message -> {});
        helper.assertTrue(Double.valueOf(0.8).equals(equal.scale(EntityTypes.BEE)), "Equal tag values must agree");
        var exact = ResolvedSettings.resolve(config(Map.of("#realsize-test:only_bee", 0.8,
                "#realsize-test:mixed", 0.9, "minecraft:bee", 1.0)), registry, message -> {});
        helper.assertTrue(Double.valueOf(1.0).equals(exact.scale(EntityTypes.BEE)),
                "Exact neutral rule wins even when overridden tags disagree");
        var warnings = new ArrayList<String>();
        var excluded = ResolvedSettings.resolve(config(Map.of("#minecraft:arrows", 0.8,
                "#minecraft:impact_projectiles", 0.9, "#realsize-test:empty", 0.7)), registry, warnings::add);
        helper.assertTrue(excluded.scale(EntityTypes.ARROW) == null, "Excluded tag overlaps cannot cause conflicts");
        helper.assertTrue(warnings.isEmpty(), "Known empty and nonliving tags are not unknown selectors");
        helper.succeed();
    }

    @GameTest
    public void unknownSelectorsWarnWithoutRewritingAuthoritativeFile(GameTestHelper helper) throws IOException {
        var directory = Files.createTempDirectory(FabricLoader.getInstance().getGameDir(), "resolution-");
        var file = directory.resolve("realsize.json");
        byte[] original = """
                {"configVersion":2,"floor":0.22,"hostileFloor":0.6,"cap":1.45,
                 "proportionalStepHeight":true,"maxStepHeight":1.49,"clientRenderDistanceFix":true,
                 "entityScales":{"minecraft:missing_realsize_type":0.4,
                    "#minecraft:missing_realsize_tag":0.5,"minecraft:bee":0.25}}
                """.getBytes(StandardCharsets.UTF_8);
        try {
            Files.write(file, original);
            var warnings = new ArrayList<String>();
            var parsed = ConfigFile.loadOrCreate(file, warnings::add);
            var settings = ResolvedSettings.resolve(parsed,
                    helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), warnings::add);
            helper.assertTrue(warnings.size() == 2, "Both unknown selectors must warn exactly once");
            helper.assertTrue(warnings.stream().anyMatch(message -> message.contains("minecraft:missing_realsize_type"))
                    && warnings.stream().anyMatch(message -> message.contains("#minecraft:missing_realsize_tag")),
                    "Warnings must name each selector");
            helper.assertTrue(parsed.entityScales().size() == 3, "Unknown selectors remain in immutable config");
            helper.assertTrue(Arrays.equals(original, Files.readAllBytes(file)), "Original bytes must remain on disk");
            helper.assertTrue(settings.scale(EntityTypes.PIG) == null, "Unknown ID cannot fall back to default type");
            helper.assertTrue(Double.valueOf(0.25).equals(settings.scale(EntityTypes.BEE)), "Known rules still resolve");
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
        helper.succeed();
    }

    @GameTest
    public void playerAndNonlivingRulesAreExcludedForExactIdsAndTags(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        var tagged = ResolvedSettings.resolve(config(Map.of("#realsize-test:mixed", 0.8)), registry, message -> {});
        helper.assertTrue(tagged.scale(EntityTypes.PLAYER) == null, "Tag must exclude players");
        helper.assertTrue(tagged.scale(EntityTypes.ITEM) == null, "Tag must exclude nonliving entities");
        helper.assertTrue(Double.valueOf(0.8).equals(tagged.scale(EntityTypes.ARMOR_STAND)),
                "Living non-Mob armor stands remain eligible");
        var rules = new HashMap<String, Double>();
        registry.forEach(type -> rules.put(registry.getKey(type).toString(), 0.8));
        var exact = ResolvedSettings.resolve(config(rules), registry, message -> {});
        for (var type : registry) {
            boolean eligible = type != EntityTypes.PLAYER
                    && type.create(helper.getLevel(), EntitySpawnReason.COMMAND) instanceof LivingEntity;
            helper.assertTrue(eligible == (exact.scale(type) != null),
                    "Eligibility must match actual 26.3 entity for " + registry.getKey(type));
        }
        helper.succeed();
    }

    @GameTest
    public void conflictingTagsFailWithNamedEntityAndSelectors(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        var previous = ResolvedSettings.resolve(config(Map.of("minecraft:bee", 0.25)), registry, message -> {});
        try {
            ResolvedSettings.resolve(config(Map.of(
                    "#realsize-test:only_bee", 0.8, "#realsize-test:mixed", 0.9)),
                    registry, message -> {});
        } catch (IllegalArgumentException exception) {
            helper.assertTrue(exception.getMessage().contains("minecraft:bee")
                    && exception.getMessage().contains("#realsize-test:only_bee")
                    && exception.getMessage().contains("#realsize-test:mixed"),
                    "Conflict must name the entity and both selectors");
            helper.assertTrue(Double.valueOf(0.25).equals(previous.scale(EntityTypes.BEE)),
                    "Failed candidate resolution cannot alter a prior snapshot");
            helper.succeed();
            return;
        }
        helper.assertTrue(false, "Conflicting tag rules must fail rather than choose a winner");
    }

    @GameTest
    public void exactIdsBeatTagsAndNeutralRulesRemainExplicit(GameTestHelper helper) {
        var settings = ResolvedSettings.resolve(config(Map.of(
                "#minecraft:skeletons", 0.8, "minecraft:skeleton", 1.0)),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), message -> {});
        helper.assertTrue(Double.valueOf(1.0).equals(settings.scale(EntityTypes.SKELETON)), "Exact neutral rule must beat tag");
        helper.assertTrue(Double.valueOf(0.8).equals(settings.scale(EntityTypes.WITHER_SKELETON)), "Other tag members must resolve");
        helper.assertTrue(settings.scale(EntityTypes.BEE) == null, "Unselected type must stay absent");
        helper.succeed();
    }

    @GameTest
    public void exactRuleResolvesAgainstServerRegistry(GameTestHelper helper) {
        var config = config(Map.of("minecraft:bee", 0.25));
        var settings = ResolvedSettings.resolve(config,
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), message -> {});
        helper.assertTrue(Double.valueOf(0.25).equals(settings.scale(EntityTypes.BEE)), "Exact bee rule must resolve");
        helper.assertTrue(settings.scale(EntityTypes.COW) == null, "Absent rule must remain absent");
        helper.assertTrue(settings.config() == config, "Snapshot must retain immutable policy inputs");
        helper.succeed();
    }

    private static RealSizeConfig config(Map<String, Double> rules) {
        return new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, true, rules);
    }
}
