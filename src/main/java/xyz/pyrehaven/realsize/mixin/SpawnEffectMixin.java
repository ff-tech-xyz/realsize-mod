package xyz.pyrehaven.realsize.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.pyrehaven.realsize.sizing.SpawnPreparation;

@Mixin(Entity.class)
abstract class SpawnEffectMixin {
    @WrapMethod(method = "removeVehicle")
    private void realsize$prepareDismount(Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        var self = (Entity) (Object) this;
        if (preparation == null || !preparation.owns(self)) original.call();
        else preparation.dismount(self, () -> original.call());
    }

    @ModifyExpressionValue(method = "canRide", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;boardingCooldown:I"))
    private int realsize$plannedCooldown(int actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : preparation.boardingCooldown((Entity) (Object) this, actual);
    }

    @WrapMethod(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z")
    private boolean realsize$prepareRide(Entity vehicle, boolean force, boolean sendEvent, Operation<Boolean> original) {
        var preparation = SpawnPreparation.current();
        var self = (Entity) (Object) this;
        if (preparation == null || !preparation.owns(self)) return original.call(vehicle, force, sendEvent);
        return preparation.ride(self, vehicle, force, () -> original.call(vehicle, force, sendEvent));
    }

    @ModifyReturnValue(method = "getVehicle", at = @At("RETURN"))
    private Entity realsize$plannedVehicle(Entity actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : preparation.vehicle((Entity) (Object) this, actual);
    }

    @ModifyReturnValue(method = "getPassengers", at = @At("RETURN"))
    private List<Entity> realsize$plannedPassengers(List<Entity> actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : preparation.passengers((Entity) (Object) this, actual);
    }

    @ModifyReturnValue(method = "isPassenger", at = @At("RETURN"))
    private boolean realsize$plannedPassenger(boolean actual) {
        return SpawnPreparation.current() == null ? actual : ((Entity) (Object) this).getVehicle() != null;
    }

    @ModifyReturnValue(method = "isVehicle", at = @At("RETURN"))
    private boolean realsize$plannedVehicleMembership(boolean actual) {
        return SpawnPreparation.current() == null ? actual : !((Entity) (Object) this).getPassengers().isEmpty();
    }

    @ModifyExpressionValue(method = {"canAddPassenger", "getFirstPassenger", "hasPassenger", "getIndirectPassengersStream", "getPassengersAndSelf"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;passengers:Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<Entity> realsize$plannedCapacity(ImmutableList<Entity> actual) {
        var preparation = SpawnPreparation.current();
        return preparation == null ? actual : ImmutableList.copyOf(preparation.passengers((Entity) (Object) this, actual));
    }

    @WrapMethod(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V")
    private void realsize$prepareSound(SoundEvent sound, float volume, float pitch, Operation<Void> original) {
        var preparation = SpawnPreparation.current();
        if (preparation == null || !preparation.owns((Entity) (Object) this)) original.call(sound, volume, pitch);
        else preparation.defer(() -> original.call(sound, volume, pitch));
    }
}
