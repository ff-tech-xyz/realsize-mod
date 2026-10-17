package xyz.pyrehaven.realsize.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pyrehaven.realsize.RealSizeMod;

/** Vanilla calls this callback only when baby metadata actually changes. */
@Mixin(value = {AgeableMob.class, Zombie.class, Piglin.class, Zoglin.class}, remap = false)
abstract class BabyStateMixin {
    @Shadow(remap = false) @Final private static EntityDataAccessor<Boolean> DATA_BABY_ID;

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void realsize$babyStateChanged(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (accessor == DATA_BABY_ID && entity.level() instanceof ServerLevel level
                && level.getEntity(entity.getId()) == entity) {
            // Construction and disk loading are reconciled by ENTITY_LOAD instead.
            RealSizeMod.currentSizing().reconcile(entity);
        }
    }
}
