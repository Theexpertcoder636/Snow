package gg.overthisnow.snow.core.layout;

/** Immutable rectangle in GUI-scaled pixels. */
public record Rect(int x, int y, int w, int h) {
    public int right() { return x + w; }
    public int bottom() { return y + h; }
    public boolean contains(int px, int py) { return px >= x && px < right() && py >= y && py < bottom(); }
    public boolean intersects(Rect o) { return x < o.right() && right() > o.x && y < o.bottom() && bottom() > o.y; }
    public Rect inset(int pad) { return new Rect(x + pad, y + pad, Math.max(0, w - 2 * pad), Math.max(0, h - 2 * pad)); }
}
