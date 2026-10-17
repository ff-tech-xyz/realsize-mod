package xyz.pyrehaven.realsize;

import java.io.IOException;
import java.io.UncheckedIOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.pyrehaven.realsize.config.ConfigFile;
import xyz.pyrehaven.realsize.config.RealSizeConfig;
import xyz.pyrehaven.realsize.config.ResolvedSettings;
import xyz.pyrehaven.realsize.sizing.EntitySizing;

public final class RealSizeMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("realsize");
    private static RealSizeMod instance;
    private EntitySizing sizing;

    public static EntitySizing currentSizing() {
        return instance.sizing;
    }

    @Override
    public void onInitialize() {
        instance = this;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                sizing = loadCandidate(server);
            } catch (IOException exception) {
                throw new UncheckedIOException("RealSize startup configuration failed", exception);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
                dispatcher.register(Commands.literal("realsize")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reload").executes(command -> reload(command.getSource())))));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (!success) {
                return;
            }
            try {
                // Datapack reloads re-resolve the active config, not uncommitted disk edits.
                activate(server, resolve(server, sizing.config()));
            } catch (IllegalArgumentException exception) {
                LOGGER.error("RealSize tag reload failed; previous settings retained: {}", exception.getMessage());
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> sizing.onLoad(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> sizing = null);
        LOGGER.info("RealSize initialized; transient entity sizing lifecycle registered.");
    }

    private EntitySizing loadCandidate(MinecraftServer server) throws IOException {
        return ConfigFile.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve("realsize.json"),
                LOGGER::warn, config -> resolve(server, config));
    }

    private EntitySizing resolve(MinecraftServer server, RealSizeConfig config) {
        return new EntitySizing(ResolvedSettings.resolve(config,
                server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), LOGGER::warn), LOGGER::warn);
    }

    private int reload(CommandSourceStack source) {
        EntitySizing candidate;
        try {
            candidate = loadCandidate(source.getServer());
        } catch (IOException exception) {
            source.sendFailure(Component.literal("RealSize reload failed; previous settings retained: " + exception.getMessage()));
            return 0;
        }
        activate(source.getServer(), candidate);
        source.sendSuccess(() -> Component.literal("RealSize reloaded; loaded mobs reconciled."), true);
        return 1;
    }

    private void activate(MinecraftServer server, EntitySizing candidate) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("RealSize settings must be activated on the server thread");
        }
        sizing = candidate;
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                sizing.reconcile(entity);
            }
        }
    }
}
