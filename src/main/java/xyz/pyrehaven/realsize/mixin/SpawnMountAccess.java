package xyz.pyrehaven.realsize.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface SpawnMountAccess {
    @Invoker("couldAcceptPassenger") boolean realsize$couldAcceptPassenger();
    @Invoker("canRide") boolean realsize$canRide(Entity vehicle);
    @Invoker("canAddPassenger") boolean realsize$canAddPassenger(Entity passenger);
}
