package xyz.pyrehaven.realsize;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;

public final class NaturalSpawnProbe {
    public record Finalized(Mob mob, SpawnGroupData group) {}
    public static final List<Finalized> finalized = new ArrayList<>();
    public static final List<Mob> created = new ArrayList<>();
    public static java.util.function.Consumer<Mob> createdHook = mob -> {};
    public static java.util.function.Consumer<Mob> constructedHook = mob -> {};
    public static java.util.function.Consumer<Mob> inner = mob -> {};
    public static java.util.function.Consumer<Mob> outer = mob -> {};
    public static int innerCalls;
    public static int outerCalls;
    public static int mountEvents;
    public static int dismountEvents;
    public static float dimensionMultiplier = 1.0f;

    public static void reset() {
        inner = mob -> {};
        outer = mob -> {};
        innerCalls = outerCalls = mountEvents = dismountEvents = 0;
        finalized.clear();
        created.clear();
        createdHook = mob -> {};
        constructedHook = mob -> {};
        dimensionMultiplier = 1.0f;
    }
    private NaturalSpawnProbe() {}
}
