package gg.overthisnow.snow.core.anim;

/** Easing curves mapping t in [0,1] to progress in [0,1]. */
public enum Easing {
    LINEAR {
        @Override public double apply(double t) { return clamp(t); }
    },
    EASE_OUT_CUBIC {
        @Override public double apply(double t) { double u = 1 - clamp(t); return 1 - u * u * u; }
    },
    EASE_IN_OUT_CUBIC {
        @Override public double apply(double t) {
            t = clamp(t);
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    };

    public abstract double apply(double t);

    static double clamp(double t) { return t < 0 ? 0 : (t > 1 ? 1 : t); }
}
