package xyz.pyrehaven.realsize.sizing;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ServerLevelAccessor;
import xyz.pyrehaven.realsize.mixin.SpawnMountAccess;

/** Effects belong to an attempt until its complete finalization expression clears. */
public final class SpawnPreparation {
    private static final ThreadLocal<SpawnPreparation> ACTIVE = new ThreadLocal<>();
    private final SpawnPreparation parent;
    private final List<Mob> fresh = new ArrayList<>();
    private final List<Runnable> effects = new ArrayList<>();
    private final IdentityHashMap<Object, Integer> pendingCounts = new IdentityHashMap<>();
    private final IdentityHashMap<Entity, Entity> vehicles = new IdentityHashMap<>();
    private final IdentityHashMap<Entity, List<Entity>> passengers = new IdentityHashMap<>();
    private final java.util.Set<Entity> dismounted = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private SpawnPreparation(Mob root) {
        parent = current();
        if (root != null) fresh.add(root);
    }

    public static SpawnPreparation current() {
        return ACTIVE.get();
    }

    public static void begin(Mob root) {
        ACTIVE.set(new SpawnPreparation(root));
    }

    public void defer(Runnable effect) {
        effects.add(effect);
    }

    public void changeCount(Object owner, int delta, Runnable effect) {
        pendingCounts.merge(owner, delta, Integer::sum);
        defer(effect);
    }

    public int count(Object owner, int actual) {
        return actual + pendingCounts.getOrDefault(owner, 0);
    }

    public void own(Entity entity) {
        if (entity instanceof Mob mob && !fresh.contains(mob)) fresh.add(mob);
    }

    public boolean owns(Entity entity) {
        return fresh.contains(entity);
    }

    public Entity vehicle(Entity entity, Entity actual) {
        return vehicles.getOrDefault(entity, actual);
    }

    public List<Entity> passengers(Entity entity, List<Entity> actual) {
        return passengers.containsKey(entity) ? List.copyOf(passengers.get(entity)) : actual;
    }

    public boolean ride(Entity rider, Entity vehicle, boolean force, Runnable commit) {
        Entity previous = rider.getVehicle();
        var access = (SpawnMountAccess) vehicle;
        if (previous == vehicle || !access.realsize$couldAcceptPassenger()
                || !vehicle.getType().canSerialize()) return false;
        for (Entity ancestor = vehicle; ancestor != null; ancestor = ancestor.getVehicle()) {
            if (ancestor == rider) return false;
        }
        if (!force && (!((SpawnMountAccess) rider).realsize$canRide(vehicle)
                || !access.realsize$canAddPassenger(rider))) return false;
        // Run vanilla's candidate-local dismount positioning now. removeVehicle
        // projects membership and queues its external effect before the new ride,
        // so accepted replay cannot perform a second positional dismount after clearance.
        if (previous != null) rider.stopRiding();
        vehicles.put(rider, vehicle);
        mutablePassengers(vehicle).add(rider);
        rider.setPose(Pose.STANDING);
        defer(commit);
        return true;
    }

    private List<Entity> mutablePassengers(Entity entity) {
        return passengers.computeIfAbsent(entity, key -> new ArrayList<>(key.getPassengers()));
    }

    public boolean clear(ServerLevelAccessor level) {
        for (var mob : fresh) SpawnSizing.prepare(mob);
        var positioned = new java.util.HashSet<Entity>();
        for (var mob : fresh) position(mob, positioned);
        return fresh.stream().allMatch(mob -> level.noCollision(mob, mob.getBoundingBox()));
    }

    public int boardingCooldown(Entity entity, int actual) {
        return dismounted.contains(entity) ? 60 : actual;
    }

    public void dismount(Entity rider, Runnable commit) {
        var previous = rider.getVehicle();
        if (previous == null) return;
        mutablePassengers(previous).remove(rider);
        vehicles.put(rider, null);
        dismounted.add(rider);
        defer(commit);
    }

    private void position(Entity entity, java.util.Set<Entity> positioned) {
        if (!positioned.add(entity)) return;
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) return;
        if (owns(vehicle)) position(vehicle, positioned);
        var point = vehicle.getPassengerRidingPosition(entity).subtract(entity.getVehicleAttachmentPoint(vehicle));
        entity.setPos(point);
    }

    public void commit() {
        restore(parent);
        effects.forEach(Runnable::run);
    }

    public static boolean finish(Mob root, ServerLevelAccessor level) {
        var preparation = current();
        if (preparation.clear(level)) {
            preparation.commit();
            return true;
        }
        preparation.abort();
        return false;
    }

    public void abort() {
        restore(parent);
        fresh.forEach(Mob::discard);
    }

    public static void closeTo(SpawnPreparation enclosing) {
        while (current() != enclosing) current().abort();
    }

    private static void restore(SpawnPreparation preparation) {
        if (preparation == null) ACTIVE.remove();
        else ACTIVE.set(preparation);
    }
}
