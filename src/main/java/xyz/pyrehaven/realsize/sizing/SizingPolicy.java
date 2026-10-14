package xyz.pyrehaven.realsize.sizing;

public record SizingPolicy(double floor, double hostileFloor, double cap) {
    public SizingPolicy {
        positive("floor", floor);
        positive("hostileFloor", hostileFloor);
        positive("cap", cap);
        if (hostileFloor < floor || cap < hostileFloor) {
            throw new IllegalArgumentException("Bounds must satisfy floor <= hostileFloor <= cap");
        }
    }

    /**
     * Returns the owned multiplier, not the final adult-relative size or an attribute amount.
     * The adapter removes its modifier when this returns 1.0. babyFactor is the vanilla
     * age multiplier (1.0 for adults), excluding RealSize and foreign attribute modifiers.
     */
    public double multiplier(Double requestedScale, boolean hostile, double babyFactor) {
        positive("babyFactor", babyFactor);
        if (requestedScale == null || requestedScale == 1.0) {
            return 1.0;
        }
        positive("requestedScale", requestedScale);
        double finalSize = Math.clamp(requestedScale * babyFactor, hostile ? hostileFloor : floor, cap);
        double multiplier = finalSize / babyFactor;
        positive("multiplier", multiplier);
        return multiplier;
    }

    public double stepHeight(double baseline, double finalScale, double maxStepHeight) {
        // Minecraft consumes step height as a float. A double just below 1.5 can
        // round up to 1.5f, so the owned ceiling must also survive that conversion.
        double ceiling = Math.min(maxStepHeight, Math.nextDown(1.5f));
        return Math.max(baseline, Math.min(baseline * Math.max(1.0, finalScale), ceiling));
    }

    private static void positive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + ": must be positive and finite");
        }
    }
}
