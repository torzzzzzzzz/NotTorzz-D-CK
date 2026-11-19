package net.nottorzz.dck.client.gui.render;

/**
 * Small collection of easing functions for smooth animations.
 * All take and return a normalised progress value in [0, 1]
 * (outputs may briefly overshoot for the "back" variants).
 */
public final class Easing {
    private Easing() {
    }

    public static float clamp01(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }

    /** Linear interpolation helper. */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float outCubic(float t) {
        t = clamp01(t);
        float f = t - 1f;
        return f * f * f + 1f;
    }

    public static float inOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4f * t * t * t
                        : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    public static float outQuint(float t) {
        t = clamp01(t);
        return 1f - (float) Math.pow(1f - t, 5);
    }

    /** Overshooting "pop" used for element entrances. */
    public static float outBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float f = t - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }

    /** Elastic settle, nice for the mascot bounce. */
    public static float outElastic(float t) {
        t = clamp01(t);
        if (t == 0f || t == 1f) {
            return t;
        }
        float c4 = (float) (2f * Math.PI) / 3f;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75f) * c4) + 1);
    }

    /** A smooth 0->1->0 pulse over the given progress. */
    public static float pulse(float t) {
        return (float) Math.sin(clamp01(t) * Math.PI);
    }
}
