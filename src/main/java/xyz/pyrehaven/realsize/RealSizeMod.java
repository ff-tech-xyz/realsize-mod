package xyz.pyrehaven.realsize;

import java.io.IOException;
import java.io.UncheckedIOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.pyrehaven.realsize.config.ConfigFile;
import xyz.pyrehaven.realsize.config.ResolvedSettings;
import xyz.pyrehaven.realsize.sizing.EntitySizing;

public final class RealSizeMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("realsize");
    private EntitySizing sizing;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                var config = ConfigFile.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve("realsize.json"),
                        LOGGER::warn);
                sizing = new EntitySizing(ResolvedSettings.resolve(config,
                        server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), LOGGER::warn), LOGGER::warn);
            } catch (IOException exception) {
                throw new UncheckedIOException("RealSize startup configuration failed", exception);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> sizing.onLoad(entity));
        // Fabric has no growth callback. Reconcile after vanilla age changes on the server,
        // without a second per-entity age cache or a server mixin.
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (var entity : level.getAllEntities()) {
                sizing.reconcile(entity);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> sizing = null);
        LOGGER.info("RealSize initialized; transient entity sizing lifecycle registered.");
    }
}
