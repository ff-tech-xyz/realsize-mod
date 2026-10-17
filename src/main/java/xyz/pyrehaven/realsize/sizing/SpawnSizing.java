package xyz.pyrehaven.realsize.sizing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import xyz.pyrehaven.realsize.RealSizeMod;

public final class SpawnSizing {
    private SpawnSizing() {}

    public static boolean clear(Mob mob, ServerLevelAccessor level) {
        prepare(mob);
        return level.noCollision(mob, mob.getBoundingBox());
    }

    public static void prepare(Mob mob) {
        RealSizeMod.currentSizing().reconcile(mob);
        // Fresh mobs have not ticked through LivingEntity.refreshDirtyAttributes yet.
        mob.refreshDimensions();
    }
}
