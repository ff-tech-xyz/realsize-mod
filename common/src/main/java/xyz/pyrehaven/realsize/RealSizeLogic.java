package xyz.pyrehaven.realsize;

public final class RealSizeLogic {
    private RealSizeLogic() {
    }

    public static double scaleForEntity(String entityId, double fallbackScale, RealSizeConfig config) {
        return config.scaleFor(entityId, fallbackScale);
    }

    public static double clampScale(double scale, RealSizeConfig config) {
        return Math.max(config.floor(), Math.min(config.cap(), scale));
    }

    public static boolean shouldBoostTracking(String entityId, double fallbackScale, RealSizeConfig config) {
        return clampScale(scaleForEntity(entityId, fallbackScale, config), config) < config.trackingRangeThreshold();
    }

    public static boolean shouldBoostStepHeight(String entityId, double fallbackScale, RealSizeConfig config) {
        return clampScale(scaleForEntity(entityId, fallbackScale, config), config) > config.stepHeightBoostThreshold();
    }
}
