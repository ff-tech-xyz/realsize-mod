package xyz.pyrehaven.realsize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import xyz.pyrehaven.realsize.config.DefaultScales;

public final class ScaffoldGameTests {
    @GameTest
    public void defaultTableContainsLiving26dot3Mobs(GameTestHelper helper) {
        var defaults = DefaultScales.config();
        for (var rule : defaults.entityScales().entrySet()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(rule.getKey()));
            helper.assertTrue(type.isPresent(), "Default ID must exist in 26.3: " + rule.getKey());
            var entity = type.orElseThrow().create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(entity instanceof Mob, "Default must name a mob: " + rule.getKey());
            double minimum = type.orElseThrow().getCategory() == MobCategory.MONSTER
                    ? defaults.hostileFloor() : defaults.floor();
            helper.assertTrue(rule.getValue() >= minimum && rule.getValue() <= defaults.cap(),
                    "Default must respect category bounds: " + rule.getKey());
        }
        helper.succeed();
    }

    @GameTest
    public void scaffoldLoadsOnDedicatedServer(GameTestHelper helper) {
        FabricLoader loader = FabricLoader.getInstance();
        helper.assertTrue(loader.getEnvironmentType() == EnvType.SERVER, "Test must run on a dedicated server");
        helper.assertTrue(loader.getEntrypoints("main", ModInitializer.class).stream()
                .anyMatch(RealSizeMod.class::isInstance), "Loader must discover the actual RealSize entrypoint");
        helper.assertTrue(loader.getModContainer("realsize").orElseThrow().getMetadata()
                .getVersion().getFriendlyString().equals("1.2.0"), "Loader must see the declared artifact version");
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        helper.assertTrue(bee.getScale() == 0.25, "Actual entrypoint must apply the GameTest fixture's bee scale");
        helper.succeed();
    }
}
