package xyz.pyrehaven.realsize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;

public final class ScaffoldGameTests {
    @GameTest
    public void scaffoldLoadsOnDedicatedServer(GameTestHelper helper) {
        FabricLoader loader = FabricLoader.getInstance();
        helper.assertTrue(loader.getEnvironmentType() == EnvType.SERVER, "Test must run on a dedicated server");
        helper.assertTrue(loader.getEntrypoints("main", ModInitializer.class).stream()
                .anyMatch(RealSizeMod.class::isInstance), "Loader must discover the actual RealSize entrypoint");
        helper.assertTrue(loader.getModContainer("realsize").orElseThrow().getMetadata()
                .getVersion().getFriendlyString().equals("1.2.0"), "Loader must see the declared artifact version");
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        helper.assertTrue(bee.getScale() == 1.0, "The scaffold must leave vanilla mob scale untouched");
        helper.succeed();
    }
}
