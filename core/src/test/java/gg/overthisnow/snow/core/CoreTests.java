package gg.overthisnow.snow.core;

import gg.overthisnow.snow.core.anim.Animated;
import gg.overthisnow.snow.core.anim.AnimationMode;
import gg.overthisnow.snow.core.anim.Easing;
import gg.overthisnow.snow.core.config.ConfigStore;
import gg.overthisnow.snow.core.layout.Rect;
import gg.overthisnow.snow.core.layout.ResponsiveLayout;
import gg.overthisnow.snow.core.notify.NotificationCenter;
import gg.overthisnow.snow.core.search.SettingsIndex;
import gg.overthisnow.snow.core.theme.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free test runner (no JUnit needed). Exit code 1 on any failure. */
public final class CoreTests {
    private static int passed, failed;

    public static void main(String[] args) throws Exception {
        animation();
        layout();
        theme();
        notifications();
        search();
        config();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  ok   " + name); }
        else { failed++; System.out.println("  FAIL " + name); }
    }

    static void animation() {
        System.out.println("animation");
        // Frame-rate independence: 60 fps vs 144 fps reach nearly same value at same elapsed time.
        Animated a = new Animated(0, 1.0, Easing.EASE_OUT_CUBIC);
        Animated b = new Animated(0, 1.0, Easing.EASE_OUT_CUBIC);
        a.setTarget(1, AnimationMode.BALANCED, false);
        b.setTarget(1, AnimationMode.BALANCED, false);
        for (int i = 0; i < 30; i++) a.update(1.0 / 60);
        for (int i = 0; i < 72; i++) b.update(1.0 / 144);
        check("same value at 0.5s regardless of fps", Math.abs(a.value() - b.value()) < 1e-9);
        check("settles at target", settle(a) == 1.0);

        Animated snap = new Animated(0, 1.0, Easing.LINEAR);
        snap.setTarget(5, AnimationMode.OFF, false);
        check("OFF mode snaps immediately", snap.value() == 5 && snap.isSettled());
        Animated rm = new Animated(0, 1.0, Easing.LINEAR);
        rm.setTarget(5, AnimationMode.FLUID, true);
        check("reduced motion snaps", rm.value() == 5);

        Animated spike = new Animated(0, 1.0, Easing.LINEAR);
        spike.setTarget(1, AnimationMode.BALANCED, false);
        spike.update(100);
        check("huge delta is clamped, not instant", spike.value() < 1.0);
        check("easing bounds", Easing.EASE_IN_OUT_CUBIC.apply(-1) == 0 && Easing.EASE_IN_OUT_CUBIC.apply(2) == 1);
    }

    static double settle(Animated a) { for (int i = 0; i < 1000; i++) a.update(0.1); return a.value(); }

    static void layout() {
        System.out.println("layout");
        check("720p-ish gui (427x240) is regular or compact, not expanded",
                ResponsiveLayout.modeFor(427, 240) != ResponsiveLayout.Mode.EXPANDED);
        check("1080p gui scale 2 (960x540) is expanded", ResponsiveLayout.modeFor(960, 540) == ResponsiveLayout.Mode.EXPANDED);
        check("tiny window is compact", ResponsiveLayout.modeFor(300, 200) == ResponsiveLayout.Mode.COMPACT);
        Rect p = ResponsiveLayout.centeredPanel(960, 540, 0.8, 0.8, 300, 200, 640, 400);
        check("panel respects max and is centered", p.w() == 640 && p.h() == 400 && p.x() == 160 && p.y() == 70);
        Rect small = ResponsiveLayout.centeredPanel(200, 150, 0.9, 0.9, 300, 200, 640, 400);
        check("panel never exceeds a tiny screen", small.w() <= 200 && small.h() <= 150);
        Rect lost = ResponsiveLayout.clampOnScreen(new Rect(5000, -300, 100, 50), 800, 450);
        check("off-screen widget is pulled back", lost.right() <= 800 && lost.x() >= 0 && lost.y() >= 0);
        check("columns at least 1", ResponsiveLayout.columns(10, 100, 4) == 1);
        check("columns fit exactly", ResponsiveLayout.columns(430, 100, 10) == 4);
        check("columns round down", ResponsiveLayout.columns(420, 100, 10) == 3);
    }

    static void theme() {
        System.out.println("theme");
        Theme t = new Theme();
        t.panelOpacity = Double.NaN; t.uiScale = 99; t.blurRadius = -5; t.cornerRadius = 500;
        t.animationMode = null; t.accent = 0xFFFFFFFF;
        t.validate();
        check("NaN and out-of-range values are clamped", t.panelOpacity >= 0.15 && t.uiScale == 2.0
                && t.blurRadius == 0 && t.cornerRadius == 16 && t.animationMode != null && t.accent == 0xFFFFFF);
        t.rgbCycle = true; t.reducedMotion = true; t.accent = 0x123456;
        check("rgb cycling is suppressed under reduced motion", t.accentAt(3.7) == 0x123456);
        t.reducedMotion = false;
        check("rgb cycle changes color over time", t.accentAt(0) != t.accentAt(2));
        t.highContrast = true; t.panelOpacity = 0.3;
        check("high contrast raises panel alpha", t.panelAlpha() >= 216);
    }

    static void notifications() {
        System.out.println("notifications");
        NotificationCenter c = new NotificationCenter();
        c.setMaxVisible(2);
        c.post(NotificationCenter.Category.CONFIG, NotificationCenter.Level.SUCCESS, "Saved", 0);
        c.post(NotificationCenter.Category.CONFIG, NotificationCenter.Level.SUCCESS, "Saved", 1);
        check("repeated event is grouped, not duplicated", c.visible().size() == 1 && c.visible().get(0).count == 2);
        c.post(NotificationCenter.Category.MODULE, NotificationCenter.Level.INFO, "A", 1);
        c.post(NotificationCenter.Category.MODULE, NotificationCenter.Level.INFO, "B", 1);
        check("max visible enforced", c.visible().size() == 2);
        c.setCategoryEnabled(NotificationCenter.Category.VISION, false);
        check("disabled category posts nothing",
                c.post(NotificationCenter.Category.VISION, NotificationCenter.Level.INFO, "x", 1) == null);
        c.update(100);
        check("notifications expire", c.visible().isEmpty());
        check("history retained after expiry", !c.history().isEmpty());
        c.clearHistory();
        check("history can be cleared", c.history().isEmpty());
    }

    static void search() {
        System.out.println("search");
        SettingsIndex idx = new SettingsIndex();
        int[] opened = {0};
        idx.register(new SettingsIndex.Entry("theme.blur", "Glass blur intensity", "Theme", "Settings > Theme",
                List.of("frosted", "gaussian"), () -> opened[0]++));
        idx.register(new SettingsIndex.Entry("crosshair.gap", "Crosshair gap", "Crosshair", "Crosshair editor",
                List.of("spacing"), () -> {}));
        check("prefix match ranks", idx.search("glass", 5).get(0).entry().id().equals("theme.blur"));
        check("keyword match", idx.search("frosted", 5).size() == 1);
        check("fuzzy subsequence", idx.search("gls", 5).size() == 1);
        check("no match returns nothing", idx.search("zzzz", 5).isEmpty());
        idx.search("blur", 5).get(0).entry().open().run();
        check("selecting a result runs its real action", opened[0] == 1);
        boolean threw = false;
        try { idx.register(new SettingsIndex.Entry("x", "x", "x", "x", List.of(), null)); }
        catch (IllegalArgumentException e) { threw = true; }
        check("entry without an action is rejected", threw);
    }

    public static final class Sample {
        public int value = 1;
        public String name = "snow";
        void validate() { value = Math.max(0, Math.min(10, value)); }
    }

    static void config() throws Exception {
        System.out.println("config");
        Path dir = Files.createTempDirectory("snowcfg");
        Path f = dir.resolve("snow.json");
        ConfigStore<Sample> store = new ConfigStore<>(f, Sample.class, 1);

        var r0 = store.load(Sample::new, Sample::validate);
        check("missing file -> defaults", r0.status() == ConfigStore.LoadStatus.CREATED_DEFAULTS);
        Sample s = new Sample(); s.value = 7;
        check("save succeeds", store.save(s));
        var r1 = store.load(Sample::new, Sample::validate);
        check("round trip", r1.value().value == 7 && r1.status() == ConfigStore.LoadStatus.LOADED);

        Files.writeString(f, "{ this is not json");
        var r2 = store.load(Sample::new, Sample::validate);
        check("malformed -> defaults + corrupt backup kept",
                r2.status() == ConfigStore.LoadStatus.RECOVERED_FROM_CORRUPT
                        && r2.value().value == 1
                        && Files.list(dir).anyMatch(p -> p.getFileName().toString().contains(".corrupt-")));

        Files.writeString(f, "{\"schemaVersion\":1,\"value\":9999}");
        var r3 = store.load(Sample::new, Sample::validate);
        check("out-of-range value is validated", r3.value().value == 10);

        Files.writeString(f, "{\"schemaVersion\":99,\"value\":3}");
        ConfigStore<Sample> s2 = new ConfigStore<>(f, Sample.class, 1);
        var r4 = s2.load(Sample::new, Sample::validate);
        check("newer schema is read-only and never overwritten",
                r4.status() == ConfigStore.LoadStatus.NEWER_SCHEMA_READ_ONLY
                        && !s2.save(new Sample()) && Files.readString(f).contains("99"));

        Path f2 = dir.resolve("mig.json");
        Files.writeString(f2, "{\"schemaVersion\":1,\"oldName\":\"legacy\"}");
        ConfigStore<Sample> s3 = new ConfigStore<>(f2, Sample.class, 2,
                root -> { root.addProperty("name", root.get("oldName").getAsString()); root.remove("oldName"); });
        var r5 = s3.load(Sample::new, Sample::validate);
        check("migration v1 -> v2 applied", r5.status() == ConfigStore.LoadStatus.MIGRATED && r5.value().name.equals("legacy"));

        Files.writeString(f2, "[1,2,3]");
        var r6 = s3.load(Sample::new, Sample::validate);
        check("non-object root recovered", r6.status() == ConfigStore.LoadStatus.RECOVERED_FROM_CORRUPT);
    }
}
