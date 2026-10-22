package gg.overthisnow.snow.client;

import gg.overthisnow.snow.core.config.ConfigStore;
import gg.overthisnow.snow.core.notify.NotificationCenter;
import gg.overthisnow.snow.core.theme.Theme;
import gg.overthisnow.snow.client.ui.SnowMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Client entrypoint. UNVERIFIED against a real 26.3 build (see README): it only uses
 * the Fabric Loader/API entrypoint interfaces, and deliberately touches no Minecraft
 * rendering or screen classes yet, because those APIs changed in 26.1+ (for example
 * GuiGraphics became GuiGraphicsExtractor) and have not been checked here.
 */
public final class SnowClient implements ClientModInitializer {
    public static final String MOD_ID = "snowclient";

    private static Theme theme;
    private static ConfigStore<Theme> themeStore;
    private static final NotificationCenter NOTIFICATIONS = new NotificationCenter();

    @Override
    public void onInitializeClient() {
        // Swap the vanilla title screen for the SNOW menu. Tick-based on purpose: it needs no
        // mixin into TitleScreen internals, whose method names change between versions.
        // CHECK: Minecraft.gui.screen() accessor name, taken from the Fabric 26.x docs.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.gui.screen() instanceof TitleScreen) {
                mc.gui.setScreen(new SnowMenuScreen());
            }
        });

        Path dir = FabricLoader.getInstance().getConfigDir().resolve("snow");
        themeStore = new ConfigStore<>(dir.resolve("theme.json"), Theme.class, 1);
        ConfigStore.LoadResult<Theme> r = themeStore.load(Theme::new, Theme::validate);
        theme = r.value();
        // Only report real outcomes; a recovery is surfaced as a warning, never as success.
        if (r.status() == ConfigStore.LoadStatus.RECOVERED_FROM_CORRUPT
                || r.status() == ConfigStore.LoadStatus.NEWER_SCHEMA_READ_ONLY) {
            NOTIFICATIONS.post(NotificationCenter.Category.WARNING, NotificationCenter.Level.WARNING,
                    r.detail(), 0);
        }
    }

    public static Theme theme() { return theme; }
    public static NotificationCenter notifications() { return NOTIFICATIONS; }

    public static boolean saveTheme() {
        try {
            return themeStore.save(theme);
        } catch (IOException e) {
            NOTIFICATIONS.post(NotificationCenter.Category.CONFIG, NotificationCenter.Level.ERROR,
                    "Could not save theme: " + e.getMessage(), 0);
            return false;
        }
    }
}
