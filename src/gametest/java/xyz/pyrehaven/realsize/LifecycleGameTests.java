package xyz.pyrehaven.realsize;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class LifecycleGameTests {
    @GameTest
    public void unchangedAdultsAreNotReconciledEveryTick(GameTestHelper helper) {
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        bee.setNoAi(true);
        bee.setNoGravity(true);
        var scale = bee.getAttribute(Attributes.SCALE);
        helper.assertTrue(bee.getScale() == 0.25f, "Entity load must apply the active rule");
        scale.removeModifier(Identifier.parse("realsize:scale"));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.invoker()
                .onEndTick(helper.getLevel());
        helper.assertTrue(bee.getScale() == 1.0f,
                "Unchanged adult must not be revisited by a world-wide tick reconciliation");
        helper.succeed();
    }

    @GameTest
    public void loadedBabyStateChangesApplyBeforeTheNextTick(GameTestHelper helper) {
        var chicken = helper.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        chicken.setBaby(true);
        helper.assertTrue(Math.abs(chicken.getScale() * chicken.getAgeScale() - 0.22) < 1e-6,
                "Baby metadata must immediately apply the final-size floor");
        chicken.setAge(0);
        helper.assertTrue(Math.abs(chicken.getScale() - 0.25) < 1e-6,
                "Growth must immediately use the adult multiplier");
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 1);
        zombie.setBaby(true);
        helper.assertTrue(Math.abs(zombie.getScale() * zombie.getAgeScale() - 0.6) < 1e-6,
                "Non-ageing baby metadata must use the same owner");
        zombie.setBaby(false);
        helper.assertTrue(Math.abs(zombie.getScale() - 0.6) < 1e-6,
                "Returning to adult metadata must immediately restore adult sizing");
        var piglin = helper.spawn(EntityTypes.PIGLIN, 1, 2, 2);
        piglin.setBaby(true);
        helper.assertTrue(Math.abs(piglin.getScale() * piglin.getAgeScale() - 0.6) < 1e-6,
                "Piglin baby metadata must use the same owner");
        piglin.setBaby(false);
        helper.assertTrue(Math.abs(piglin.getScale() - 0.6) < 1e-6, "Adult piglin must restore sizing immediately");
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 2, 2, 2);
        zoglin.setBaby(true);
        helper.assertTrue(Math.abs(zoglin.getScale() * zoglin.getAgeScale() - 0.6) < 1e-6,
                "Zoglin baby metadata must use the same owner");
        zoglin.setBaby(false);
        helper.assertTrue(Math.abs(zoglin.getScale() - 0.6) < 1e-6, "Adult zoglin must restore sizing immediately");
        helper.succeed();
    }
}
