package gg.overthisnow.snow.core.anim;

/**
 * A value that eases toward a target using elapsed real time, so behaviour is
 * independent of frame rate. Call {@link #update(double)} once per frame with the
 * frame delta in seconds.
 */
public final class Animated {
    private final Easing easing;
    private final double baseDuration;
    private double from;
    private double to;
    private double elapsed;
    private double current;
    private double duration;

    public Animated(double initial, double baseDurationSeconds, Easing easing) {
        this.current = initial;
        this.from = initial;
        this.to = initial;
        this.baseDuration = baseDurationSeconds;
        this.easing = easing;
        this.duration = baseDurationSeconds;
    }

    /** Retarget. In OFF mode or reduced motion the value snaps immediately. */
    public void setTarget(double target, AnimationMode mode, boolean reducedMotion) {
        if (target == to) return;
        double scale = reducedMotion ? 0.0 : mode.durationScale();
        this.from = current;
        this.to = target;
        this.elapsed = 0;
        this.duration = baseDuration * scale;
        if (duration <= 0) {
            current = target;
            from = target;
        }
    }

    public double update(double deltaSeconds) {
        if (current == to) return current;
        if (deltaSeconds < 0) deltaSeconds = 0;
        // Clamp huge deltas (e.g. after a lag spike or pause) so they cannot overshoot logic.
        elapsed += Math.min(deltaSeconds, 0.25);
        if (duration <= 0 || elapsed >= duration) {
            current = to;
        } else {
            current = from + (to - from) * easing.apply(elapsed / duration);
        }
        return current;
    }

    public double value() { return current; }
    public double target() { return to; }
    public boolean isSettled() { return current == to; }
}
