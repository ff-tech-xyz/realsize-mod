package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(EntityType.class)
abstract class SpawnConstructionMixin {
    @ModifyReturnValue(method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private Entity realsize$ownFresh(Entity result) {
        var preparation = SpawnPreparation.current();
        if (preparation != null) preparation.own(result);
        return result;
    }
}
