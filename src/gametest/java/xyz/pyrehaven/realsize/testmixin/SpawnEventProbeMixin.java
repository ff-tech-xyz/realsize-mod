package xyz.pyrehaven.realsize.testmixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pyrehaven.realsize.NaturalSpawnProbe;

@Mixin(ServerLevel.class)
abstract class SpawnEventProbeMixin {
    @Inject(method = "gameEvent", at = @At("HEAD"))
    private void realsizeTest$events(Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
        if (event == GameEvent.ENTITY_MOUNT) NaturalSpawnProbe.mountEvents++;
        if (event == GameEvent.ENTITY_DISMOUNT) NaturalSpawnProbe.dismountEvents++;
    }
}
