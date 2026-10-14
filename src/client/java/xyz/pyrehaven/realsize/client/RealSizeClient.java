package xyz.pyrehaven.realsize.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RealSizeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("realsize/client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("RealSize client scaffold initialized; culling correction is not implemented yet.");
    }
}
