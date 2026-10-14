package xyz.pyrehaven.realsize.sizing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SizingPolicyTest {
    private final SizingPolicy policy = new SizingPolicy(0.22, 0.6, 1.45);

    @Test
    void exactBoundsAndAdjacentValuesUseTheConfiguredCategory() {
        for (double babyFactor : new double[]{1.0, 0.5}) {
            for (boolean hostile : new boolean[]{false, true}) {
                double floor = hostile ? 0.6 : 0.22;
                for (double boundary : new double[]{floor, 1.45}) {
                    for (double finalSize : new double[]{Math.nextDown(boundary), boundary, Math.nextUp(boundary)}) {
                        double expected = Math.max(floor, Math.min(1.45, finalSize));
                        assertFinalSize(policy, finalSize / babyFactor, hostile, babyFactor, expected);
                    }
                }
            }
        }
        SizingPolicy custom = new SizingPolicy(0.3, 0.7, 1.8);
        assertFinalSize(custom, 0.2, false, 0.5, 0.3);
        assertFinalSize(custom, 0.2, true, 0.5, 0.7);
        assertFinalSize(custom, 4.0, true, 0.5, 1.8);
        SizingPolicy equalBounds = new SizingPolicy(0.7, 0.7, 0.7);
        assertFinalSize(equalBounds, 2.0, false, 0.5, 0.7);
        assertEquals(1.0, equalBounds.multiplier(1.0, true, 1.0));
    }

    @Test
    void growthRecalculatesTheSameRuleAndExtremeRequestsRemainBounded() {
        assertEquals(1.2, policy.multiplier(0.8, true, 0.5), 1e-12);
        assertEquals(0.8, policy.multiplier(0.8, true, 1.0), 1e-12);
        for (boolean hostile : new boolean[]{false, true}) {
            assertFinalSize(policy, Double.MIN_VALUE, hostile, 0.5, hostile ? 0.6 : 0.22);
            assertFinalSize(policy, Double.MAX_VALUE, hostile, 1.0, 1.45);
        }
    }

    @Test
    void invalidInputsFailInsteadOfProducingInvalidModifiers() {
        for (double invalid : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new SizingPolicy(invalid, 0.6, 1.45));
            assertThrows(IllegalArgumentException.class, () -> new SizingPolicy(0.22, invalid, 1.45));
            assertThrows(IllegalArgumentException.class, () -> new SizingPolicy(0.22, 0.6, invalid));
            assertThrows(IllegalArgumentException.class, () -> policy.multiplier(invalid, false, 0.5));
            assertThrows(IllegalArgumentException.class, () -> policy.multiplier(0.4, false, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> new SizingPolicy(0.7, 0.6, 1.45));
        assertThrows(IllegalArgumentException.class, () -> new SizingPolicy(0.22, 0.6, 0.5));
        assertThrows(IllegalArgumentException.class, () -> policy.multiplier(0.4, false, Double.MIN_VALUE));
    }

    @Test
    void activeBabyRulesBoundFinalSizeRatherThanTheRequestedMultiplier() {
        assertFinalSize(policy, 0.25, false, 0.5, 0.22);
        assertFinalSize(policy, 0.8, true, 0.5, 0.6);
        assertFinalSize(policy, 1.2, false, 0.5, 0.6);
        assertFinalSize(policy, 2.0, false, 0.5, 1.0);
        assertFinalSize(policy, 4.0, true, 0.5, 1.45);
        assertFinalSize(policy, 0.01, false, 0.1, 0.22);
        assertFinalSize(policy, 0.01, true, 0.1, 0.6);
    }

    @Test
    void activeAdultRulesRespectCategoryFloorsAndCap() {
        assertFinalSize(policy, 0.01, false, 1.0, 0.22);
        assertFinalSize(policy, 0.01, true, 1.0, 0.6);
        assertFinalSize(policy, 0.4, false, 1.0, 0.4);
        assertFinalSize(policy, 0.4, true, 1.0, 0.6);
        assertFinalSize(policy, 1.2, true, 1.0, 1.2);
        assertFinalSize(policy, 2.0, false, 1.0, 1.45);
        assertFinalSize(policy, 2.0, true, 1.0, 1.45);
    }

    private static void assertFinalSize(SizingPolicy policy, double requested, boolean hostile,
                                        double babyFactor, double expected) {
        assertEquals(expected, babyFactor * policy.multiplier(requested, hostile, babyFactor), 1e-12);
    }

    @Test
    void absentAndNeutralRulesRemoveOwnedScalingForAdultsAndBabies() {
        for (boolean hostile : new boolean[]{false, true}) {
            for (double babyFactor : new double[]{1.0, 0.5, 0.1}) {
                assertEquals(1.0, policy.multiplier(null, hostile, babyFactor));
                assertEquals(1.0, policy.multiplier(1.0, hostile, babyFactor));
            }
        }
    }
}
