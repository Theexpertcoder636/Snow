package gg.overthisnow.snow.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing helpers built only on calls confirmed in the Fabric docs for 26.x:
 * GuiGraphicsExtractor.fill(x1,y1,x2,y2,argb) and outline(x,y,w,h,argb).
 * Real background blur is not available through these, so glass uses a translucent fill.
 */
public final class Draw {
    private Draw() {}

    public static int argb(int alpha255, int rgb) {
        int a = Math.max(0, Math.min(255, alpha255));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Filled rect with chamfered corners (the closest cheap stand-in for rounding). */
    public static void chamferFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int c, int color) {
        c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
        if (c == 0) { g.fill(x, y, x + w, y + h, color); return; }
        // Three non-overlapping strips so translucent colors do not double up.
        g.fill(x + c, y, x + w - c, y + c, color);
        g.fill(x, y + c, x + w, y + h - c, color);
        g.fill(x + c, y + h - c, x + w - c, y + h, color);
    }

    /** One-pixel border following the chamfer. */
    public static void chamferBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int c, int color) {
        c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
        g.fill(x + c, y, x + w - c, y + 1, color);               // top
        g.fill(x + c, y + h - 1, x + w - c, y + h, color);       // bottom
        g.fill(x, y + c, x + 1, y + h - c, color);               // left
        g.fill(x + w - 1, y + c, x + w, y + h - c, color);       // right
        for (int i = 0; i < c; i++) {                             // diagonal corners
            g.fill(x + c - 1 - i, y + i, x + c - i, y + i + 1, color);
            g.fill(x + w - c + i, y + i, x + w - c + i + 1, y + i + 1, color);
            g.fill(x + c - 1 - i, y + h - 1 - i, x + c - i, y + h - i, color);
            g.fill(x + w - c + i, y + h - 1 - i, x + w - c + i + 1, y + h - i, color);
        }
    }

    /** Glass panel: soft shadow, translucent body, faint top highlight, fine border. */
    public static void glassPanel(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                  int radius, int bodyAlpha, double borderOpacity,
                                  double shadow, int borderRgb) {
        int c = Math.min(3, radius / 3);
        if (shadow > 0) {
            int sa = (int) Math.round(shadow * 70);
            chamferFill(g, x - 2, y + 2, w + 4, h + 4, c + 1, argb(sa / 2, 0x000000));
            chamferFill(g, x, y + 4, w, h + 2, c, argb(sa, 0x000000));
        }
        chamferFill(g, x, y, w, h, c, argb(bodyAlpha, 0x0E1015));
        g.fill(x + c, y + 1, x + w - c, y + 2, argb(26, 0xFFFFFF)); // top highlight
        int ba = (int) Math.round(borderOpacity * 255);
        if (ba > 0) chamferBorder(g, x, y, w, h, c, argb(ba, borderRgb));
    }
}
