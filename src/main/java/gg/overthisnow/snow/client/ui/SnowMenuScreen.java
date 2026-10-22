package gg.overthisnow.snow.client.ui;

import gg.overthisnow.snow.client.SnowClient;
import gg.overthisnow.snow.core.anim.Easing;
import gg.overthisnow.snow.core.layout.Rect;
import gg.overthisnow.snow.core.layout.ResponsiveLayout;
import gg.overthisnow.snow.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * SNOW main menu. Port of preview/main-menu.html.
 *
 * UNVERIFIED: written from the Fabric 26.x docs but never compiled. Items most likely to need a
 * fix on the first CI build are flagged "CHECK" below.
 *
 * Uses real vanilla Button widgets (confirmed API) drawn over a custom glass panel. Fully custom
 * glass buttons, the distressed logo font and button stagger animation come after this builds.
 */
public final class SnowMenuScreen extends Screen {
    private static final String VERSION = "SNOW Client 0.1.0 · Minecraft 26.3";

    private final long openedNanos = System.nanoTime();
    private final Random starRng = new Random(26301);
    private final float[] starX = new float[140], starY = new float[140], starS = new float[140];
    private final float[] flakeX = new float[60], flakeY = new float[60], flakeV = new float[60];
    private final int[][] terrain = new int[3][512];

    private Rect panel = new Rect(0, 0, 0, 0);
    private int ringCx, ringCy, ringR;
    private ResponsiveLayout.Mode mode = ResponsiveLayout.Mode.REGULAR;

    public SnowMenuScreen() {
        super(Component.literal("SNOW"));
        for (int i = 0; i < starX.length; i++) {
            starX[i] = starRng.nextFloat(); starY[i] = starRng.nextFloat() * 0.62f; starS[i] = 0.5f + starRng.nextFloat() * 1.5f;
        }
        for (int i = 0; i < flakeX.length; i++) {
            flakeX[i] = starRng.nextFloat(); flakeY[i] = starRng.nextFloat(); flakeV[i] = 0.02f + starRng.nextFloat() * 0.05f;
        }
        int[] amp = {7, 6, 4};
        for (int l = 0; l < 3; l++) {
            Random r = new Random(900 + l);
            int v = 0;
            for (int k = 0; k < terrain[l].length; k++) {
                v += Math.round((r.nextFloat() - 0.5f) * 3);
                v = Math.max(-amp[l], Math.min(amp[l], v));
                terrain[l][k] = v;
            }
        }
    }

    @Override
    protected void init() {
        mode = ResponsiveLayout.modeFor(width, height);
        boolean compact = mode == ResponsiveLayout.Mode.COMPACT;

        int panelW = compact ? Math.min(width - 16, 220) : 214;
        int btnH = 20, gap = 4;
        int rows = 5;
        int panelH = 12 + rows * btnH + (rows - 1) * gap + 12;
        ringR = compact ? 30 : 52;
        int blockH = ringR * 2 + 6 + panelH;
        int top = Math.max(6, (height - blockH) / 2);
        ringCx = width / 2;
        ringCy = top + ringR;
        panel = new Rect((width - panelW) / 2, top + ringR * 2 + 6, panelW, panelH);

        int x = panel.x() + 12, w = panel.w() - 24;
        int half = (w - gap) / 2;
        int y = panel.y() + 12;

        Minecraft mc = Minecraft.getInstance();
        // CHECK: constructor signatures of these vanilla screens in 26.3.
        addRenderableWidget(Button.builder(Component.literal("Singleplayer"),
                b -> mc.gui.setScreen(new SelectWorldScreen(this))).bounds(x, y, half, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Multiplayer"),
                b -> mc.gui.setScreen(new JoinMultiplayerScreen(this))).bounds(x + half + gap, y, half, btnH).build());
        y += btnH + gap;
        addRenderableWidget(Button.builder(Component.literal("Settings"),
                b -> mc.gui.setScreen(new OptionsScreen(this, mc.options))).bounds(x, y, w, btnH).build());
        y += btnH + gap;
        // Honest placeholders: these screens are not built yet, so they are disabled, not faked.
        addDisabled("Profiles", x, y, half, btnH);
        addDisabled("SNOW Vision", x + half + gap, y, half, btnH);
        y += btnH + gap;
        addRenderableWidget(Button.builder(Component.literal("Quit game"),
                b -> mc.stop()).bounds(x, y, w, btnH).build());
    }

    private void addDisabled(String label, int x, int y, int w, int h) {
        Button b = Button.builder(Component.literal(label + " (soon)"), btn -> {}).bounds(x, y, w, h).build();
        b.active = false;
        addRenderableWidget(b);
    }

    /** Intro progress 0..1 for a stage; snaps to 1 when animations are off or reduced. */
    private double stage(double t, double delay, double dur) {
        Theme th = SnowClient.theme();
        double scale = th.reducedMotion ? 0 : th.animationMode.durationScale();
        if (scale <= 0) return 1;
        return Easing.EASE_OUT_CUBIC.apply((t - delay * scale) / (dur * scale));
    }

    /**
     * CHECK: assumed name of the background hook in 26.x (the old renderBackground). If the build
     * says it does not override anything, find the current name in Screen and rename this method.
     * Everything is drawn here so the vanilla background does not paint over it.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme th = SnowClient.theme();
        double t = (System.nanoTime() - openedNanos) / 1e9;
        boolean fx = th.backgroundEffects && !th.reducedMotion && th.animationMode.durationScale() > 0;
        double bg = stage(t, 0.0, 0.7);

        // Sky
        g.fillGradient(0, 0, width, height, Draw.argb(255, 0x05060A), Draw.argb(255, 0x0C0F16));
        // Stars
        for (int i = 0; i < starX.length; i++) {
            double tw = fx ? 0.5 + 0.5 * Math.sin(t * starS[i] + i) : 0.6;
            int a = (int) ((90 + 130 * tw) * bg);
            int sx = (int) (starX[i] * width), sy = (int) (starY[i] * height);
            g.fill(sx, sy, sx + 1, sy + 1, Draw.argb(a, 0xEBF0FF));
        }
        // Blocky terrain, three depth layers (static; parallax comes later).
        int block = Math.max(4, width / 70);
        double[] base = {0.74, 0.82, 0.92};
        int[] shade = {0x0B0D12, 0x07080C, 0x040506};
        for (int l = 0; l < 3; l++) {
            int cols = width / block + 2;
            for (int c = 0; c < cols; c++) {
                int top = (int) (height * base[l]) + terrain[l][c % terrain[l].length] * block / 2;
                g.fill(c * block, top, (c + 1) * block + 1, height, Draw.argb((int) (255 * bg), shade[l]));
            }
        }
        // Snow
        if (fx) {
            for (int i = 0; i < flakeX.length; i++) {
                int fy = (int) (((flakeY[i] + t * flakeV[i]) % 1.0) * height);
                int fxp = (int) (flakeX[i] * width);
                g.fill(fxp, fy, fxp + 1, fy + 1, Draw.argb(140, 0xFFFFFF));
            }
        }

        // Ring around the logo: thin accent arc that draws itself.
        double ringP = stage(t, 0.6, 1.1);
        int accent = th.accentAt(t);
        int pts = 220, drawn = (int) (pts * ringP);
        for (int i = 0; i < drawn; i++) {
            double ang = -Math.PI / 2 + (i / (double) pts) * Math.PI * 2;
            int px = ringCx + (int) Math.round(Math.cos(ang) * ringR);
            int py = ringCy + (int) Math.round(Math.sin(ang) * ringR);
            g.fill(px, py, px + 1, py + 1, Draw.argb(215, accent));
        }

        // Wordmark. CHECK: plain font for now; the distressed logo font is a resource-pack font task.
        double logoP = stage(t, 0.35, 0.65);
        int la = (int) (255 * logoP);
        if (la > 3) {
            String word = "S N O W";
            g.text(font, word, ringCx - font.width(word) / 2, ringCy - font.lineHeight / 2 - 4,
                    Draw.argb(la, 0xECEEF2), true);
            if (mode != ResponsiveLayout.Mode.COMPACT) {
                String sub = "O V E R T H I S N O W";
                g.text(font, sub, ringCx - font.width(sub) / 2, ringCy + font.lineHeight / 2 + 4,
                        Draw.argb((int) (la * 0.6), 0x9AA1AD), false);
            }
        }

        // Glass menu panel
        double panelP = stage(t, 1.0, 0.5);
        int bodyAlpha = (int) (th.panelAlpha() * panelP);
        if (bodyAlpha > 2) {
            Draw.glassPanel(g, panel.x(), panel.y(), panel.w(), panel.h(),
                    th.cornerRadius, bodyAlpha, th.borderOpacity * panelP, th.shadowIntensity * panelP, 0xFFFFFF);
        }

        if (mode != ResponsiveLayout.Mode.COMPACT) {
            g.text(font, VERSION, (width - font.width(VERSION)) / 2, Math.min(height - 12, panel.bottom() + 8),
                    Draw.argb(140, 0x9AA1AD), false);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
