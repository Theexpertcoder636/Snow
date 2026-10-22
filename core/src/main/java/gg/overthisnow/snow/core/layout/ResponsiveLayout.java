package gg.overthisnow.snow.core.layout;

/**
 * Layout rules shared by screens. Works in GUI-scaled pixels (what Minecraft's
 * Screen width/height report), so it already accounts for the GUI scale setting.
 */
public final class ResponsiveLayout {
    public enum Mode { COMPACT, REGULAR, EXPANDED }

    public static final int COMPACT_MAX_WIDTH = 360;
    public static final int EXPANDED_MIN_WIDTH = 640;

    private ResponsiveLayout() {}

    public static Mode modeFor(int screenW, int screenH) {
        if (screenW < COMPACT_MAX_WIDTH || screenH < 200) return Mode.COMPACT;
        if (screenW >= EXPANDED_MIN_WIDTH && screenH >= 320) return Mode.EXPANDED;
        return Mode.REGULAR;
    }

    /** Centered panel with width/height as a fraction of the screen, bounded by min/max. */
    public static Rect centeredPanel(int screenW, int screenH,
                                     double fracW, double fracH,
                                     int minW, int minH, int maxW, int maxH) {
        int w = bound((int) Math.round(screenW * fracW), minW, maxW);
        int h = bound((int) Math.round(screenH * fracH), minH, maxH);
        // Never exceed the screen even if the minimum is larger than the screen.
        w = Math.min(w, screenW);
        h = Math.min(h, screenH);
        return new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
    }

    /** Keep a rect fully on screen, shrinking only if it cannot fit at all. */
    public static Rect clampOnScreen(Rect r, int screenW, int screenH) {
        int w = Math.min(r.w(), screenW);
        int h = Math.min(r.h(), screenH);
        int x = bound(r.x(), 0, Math.max(0, screenW - w));
        int y = bound(r.y(), 0, Math.max(0, screenH - h));
        return new Rect(x, y, w, h);
    }

    /** Number of equal-width columns that fit, at least 1. */
    public static int columns(int availableW, int minCellW, int gap) {
        if (availableW <= 0 || minCellW <= 0) return 1;
        return Math.max(1, (availableW + gap) / (minCellW + gap));
    }

    private static int bound(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
