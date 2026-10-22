package gg.overthisnow.snow.core.anim;

/**
 * User-facing animation intensity. OFF (or reduced motion) makes every animation
 * snap to its target, so no interaction ever depends on an animation finishing.
 */
public enum AnimationMode {
    OFF(0.0),
    BALANCED(1.0),
    FLUID(1.6);

    private final double durationScale;

    AnimationMode(double durationScale) { this.durationScale = durationScale; }

    /** Multiplier applied to base durations. 0 means "snap". */
    public double durationScale() { return durationScale; }

    public static AnimationMode parse(String s, AnimationMode fallback) {
        if (s == null) return fallback;
        try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}
