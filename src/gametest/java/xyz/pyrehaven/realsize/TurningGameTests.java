package xyz.pyrehaven.realsize;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class TurningGameTests {
    @GameTest
    public void tinySpiderCompletesCorneredPath(GameTestHelper helper) {
        var origin = helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0));
        var level = helper.getLevel();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 3; y++) level.setBlockAndUpdate(origin.offset(x, y, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
        var spider = helper.spawn(EntityTypes.SPIDER, 1, 1, 1);
        spider.setNoAi(true);
        spider.getAttribute(Attributes.SCALE).removeModifiers();
        spider.getAttribute(Attributes.SCALE).setBaseValue(0.26);
        spider.refreshDimensions();
        spider.setPos(origin.getX() + 1.5, origin.getY(), origin.getZ() + 1.5);
        spider.setOnGround(true);
        spider.setYRot(180);
        var nodes = new java.util.ArrayList<net.minecraft.world.level.pathfinder.Node>();
        for (int x = 1; x <= 5; x++) nodes.add(new net.minecraft.world.level.pathfinder.Node(
                origin.getX() + x, origin.getY(), origin.getZ() + 1));
        for (int z = 2; z <= 5; z++) nodes.add(new net.minecraft.world.level.pathfinder.Node(
                origin.getX() + 5, origin.getY(), origin.getZ() + z));
        for (int x = 4; x >= 1; x--) nodes.add(new net.minecraft.world.level.pathfinder.Node(
                origin.getX() + x, origin.getY(), origin.getZ() + 5));
        var destination = origin.offset(1, 0, 5);
        var path = new net.minecraft.world.level.pathfinder.Path(nodes, destination, true);
        helper.assertTrue(spider.getNavigation().moveTo(path, 1), "Real navigation must accept the cornered path");
        int ticks = 0;
        for (; ticks < 240 && !spider.getNavigation().isDone(); ticks++) {
            spider.getNavigation().tick();
            float before = spider.getYRot();
            spider.getMoveControl().tick();
            helper.assertTrue(Math.abs(Mth.wrapDegrees(spider.getYRot() - before)) <= 35.001,
                    "Every path steering update must honor the turn cap");
            spider.travel(new net.minecraft.world.phys.Vec3(spider.xxa, spider.yya, spider.zza));
        }
        helper.assertTrue(path.isDone() && !spider.getNavigation().isStuck(),
                "Tiny spider must finish the cornered path without circling or timing out; ticks=" + ticks);
        helper.assertTrue(spider.position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(destination)) < 0.9,
                "Completing navigation must actually reach the last waypoint");
        System.out.println("REALSIZE NAVIGATION PASS: scale=" + spider.getScale() + ", ticks=" + ticks
                + ", final=" + spider.position());
        helper.succeed();
    }
    @GameTest
    public void smallSpidersTurnWithoutChangingSpeed(GameTestHelper helper) {
        var spider = helper.spawn(EntityTypes.SPIDER, 1, 2, 1);
        spider.setNoAi(true);
        double movement = spider.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (float scale : new float[]{0.26f, 1.0f, 1.45f}) {
            spider.getAttribute(Attributes.SCALE).removeModifiers();
            spider.getAttribute(Attributes.SCALE).setBaseValue(scale);
            for (float start : new float[]{0, 350, 10}) {
                spider.setYRot(start);
                // A target 90 degrees counterclockwise, including wraparound.
                double radians = Math.toRadians(start);
                spider.getMoveControl().setWantedPosition(spider.getX() + Math.cos(radians) * 4,
                        spider.getY(), spider.getZ() + Math.sin(radians) * 4, 1.0);
                spider.getMoveControl().tick();
                float delta = Mth.wrapDegrees(spider.getYRot() - start);
                float expected = scale < 1 ? -35 : -90;
                helper.assertTrue(Math.abs(delta - expected) < 0.001,
                        "Yaw must honor scale-specific turn limit: scale=" + scale + ", delta=" + delta);
                helper.assertTrue(Math.abs(spider.getSpeed() - movement) < 1e-6,
                        "Turn correction must not change vanilla movement speed");
            }
        }
        helper.succeed();
    }
}
