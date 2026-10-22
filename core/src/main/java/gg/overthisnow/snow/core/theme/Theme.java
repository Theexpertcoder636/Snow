package gg.overthisnow.snow.core.theme;

import gg.overthisnow.snow.core.anim.AnimationMode;

/**
 * Single source of truth for look and feel. Every screen and widget reads from this.
 * Mutable on purpose (bound to settings UI); call {@link #validate()} after loading.
 * Colors are 0xRRGGBB.
 */
public final class Theme {
    public int accent = 0xE6E6E6;          // restrained near-white default
    public int accentSecondary = 0x8A8F98; // gradient end
    public boolean rgbCycle = false;
    public double rgbCycleSpeed = 0.15;    // hue rotations per second

    public double panelOpacity = 0.55;     // glass surface alpha
    public int blurRadius = 8;             // 0 disables blur (translucent fallback only)
    public double borderOpacity = 0.18;
    public int borderThickness = 1;
    public int cornerRadius = 6;
    public double shadowIntensity = 0.35;
    public double glowIntensity = 0.0;
    public double uiScale = 1.0;
    public double textScale = 1.0;

    public AnimationMode animationMode = AnimationMode.BALANCED;
    public boolean reducedMotion = false;
    public boolean backgroundEffects = true;
    public boolean highContrast = false;

    public void validate() {
        accent &= 0xFFFFFF;
        accentSecondary &= 0xFFFFFF;
        rgbCycleSpeed = clamp(rgbCycleSpeed, 0.01, 2.0);
        panelOpacity = clamp(panelOpacity, 0.15, 0.95);
        blurRadius = (int) clamp(blurRadius, 0, 24);
        borderOpacity = clamp(borderOpacity, 0.0, 1.0);
        borderThickness = (int) clamp(borderThickness, 0, 4);
        cornerRadius = (int) clamp(cornerRadius, 0, 16);
        shadowIntensity = clamp(shadowIntensity, 0.0, 1.0);
        glowIntensity = clamp(glowIntensity, 0.0, 0.6);
        uiScale = clamp(uiScale, 0.5, 2.0);
        textScale = clamp(textScale, 0.75, 1.75);
        if (animationMode == null) animationMode = AnimationMode.BALANCED;
    }

    /** Effective accent at time t (seconds): static, or hue-cycled when enabled. */
    public int accentAt(double timeSeconds) {
        if (!rgbCycle || reducedMotion) return accent;
        float hue = (float) ((timeSeconds * rgbCycleSpeed) % 1.0);
        return hsvToRgb(hue, 0.55, 1.0);
    }

    /** Own HSV conversion: avoids java.awt, which is unavailable in some launcher JREs. */
    static int hsvToRgb(double h, double s, double v) {
        double h6 = (h - Math.floor(h)) * 6.0;
        int i = (int) Math.floor(h6);
        double f = h6 - i;
        double p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        double r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) Math.round(r * 255) << 16) | ((int) Math.round(g * 255) << 8) | (int) Math.round(b * 255);
    }

    /** Alpha (0-255) for glass panels; high contrast raises it for readability. */
    public int panelAlpha() {
        double a = highContrast ? Math.max(panelOpacity, 0.85) : panelOpacity;
        return (int) Math.round(a * 255);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
