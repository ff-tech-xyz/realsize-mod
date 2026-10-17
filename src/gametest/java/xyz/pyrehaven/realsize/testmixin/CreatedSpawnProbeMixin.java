package xyz.pyrehaven.realsize.testmixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.NaturalSpawnProbe;

@Mixin(EntityType.class)
abstract class CreatedSpawnProbeMixin {
    /** Foreign mods may change dimensions without changing the scale attribute. */
    @Mixin(net.minecraft.world.entity.LivingEntity.class)
    public static abstract class Dimensions {
        @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
        private net.minecraft.world.entity.EntityDimensions realsizeTest$foreignDimensions(net.minecraft.world.entity.EntityDimensions actual) {
            return NaturalSpawnProbe.created.contains((Object) this) ? actual.scale(NaturalSpawnProbe.dimensionMultiplier) : actual;
        }
    }

    @ModifyReturnValue(method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private Entity realsizeTest$constructed(Entity entity) {
        if (entity instanceof Mob mob) NaturalSpawnProbe.constructedHook.accept(mob);
        return entity;
    }

    @ModifyReturnValue(method = "create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private Entity realsizeTest$created(Entity entity) {
        if (entity instanceof Mob mob) {
            NaturalSpawnProbe.created.add(mob);
            NaturalSpawnProbe.createdHook.accept(mob);
        }
        return entity;
    }
}
