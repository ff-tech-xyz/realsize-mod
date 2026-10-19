package xyz.pyrehaven.realsize.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.pyrehaven.realsize.config.ConfigFile;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class RealSizeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("realsize/client");
    private static boolean renderDistanceFix;
    private static boolean walkTempoFix;

    public static boolean walkTempoFixEnabled() {
        return walkTempoFix;
    }

    public static boolean renderDistanceFixEnabled() {
        return renderDistanceFix;
    }

    public static void reloadOptions() throws IOException {
        var config = ConfigFile.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve("realsize.json"),
                LOGGER::warn);
        renderDistanceFix = config.clientRenderDistanceFix();
        walkTempoFix = config.clientWalkTempoFix();
    }

    @Override
    public void onInitializeClient() {
        try {
            reloadOptions();
        } catch (IOException exception) {
            throw new UncheckedIOException("RealSize client configuration failed", exception);
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("realsize-client").then(ClientCommands.literal("reload")
                        .executes(command -> {
                            try {
                                reloadOptions();
                                command.getSource().sendFeedback(Component.literal(
                                        "RealSize client corrections: distance=" + renderDistanceFix
                                                + ", walk tempo=" + walkTempoFix));
                                return 1;
                            } catch (IOException exception) {
                                command.getSource().sendError(Component.literal(
                                        "RealSize client reload failed; previous options retained: " + exception.getMessage()));
                                return 0;
                            }
                        }))));
        LOGGER.info("RealSize client initialized; local corrections: distance={}, walk tempo={}",
                renderDistanceFix, walkTempoFix);
    }
}
