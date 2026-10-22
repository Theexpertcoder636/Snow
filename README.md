# SNOW Client (OVERTHISNOW) — foundation build

Status: **early foundation. Not a finished client.** No screens, HUD, or SNOW Vision exist yet.

## Targets (from public release notes, not from a local build)
- Minecraft Java 26.3 (released Sep 15, 2026), Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3
- Java 25 (required by 26.x), Gradle 9.6+, Fabric Loom 1.17 (plugin id `net.fabricmc.fabric-loom`, no mappings line)

## Layout
- `core/` pure Java, no Minecraft dependency (compiles on JDK 21+, unit-tested):
  animation (`Animated`, `Easing`, `AnimationMode`), `Theme`, `ResponsiveLayout`, `ConfigStore`
  (versioned, atomic, corrupt-file recovery, migrations), `NotificationCenter`, `SettingsIndex`.
- `src/` Fabric mod glue. Currently only `SnowClient` (loads theme config, owns notification center).

## Test report (what was actually run)
- Ran: `core` compiled with javac (JDK 21) and `CoreTests` executed: **39 passed, 0 failed**.
  Covers frame-rate-independent animation, snap on OFF/reduced motion, layout clamping and breakpoints,
  theme validation, notification grouping/caps/expiry, search ranking and action wiring, config
  round trip, malformed-file recovery with backup, newer-schema protection, and migration.
- Gson for that run came from a local jar; the Gradle build itself was **not** run.
- **Not run:** the Fabric mod (`./gradlew build`). This workspace has Java 21 only and cannot reach
  maven.fabricmc.net or Mojang servers, so `SnowClient`, `build.gradle`, and `fabric.mod.json` are
  **uncompiled and unverified**. Minecraft was never launched.

## Build locally
1. Install JDK 25 and Gradle 9.6+ (or add a wrapper: `gradle wrapper`).
2. `gradle :core:build` then `gradle build`; jar lands in `build/libs/`. Drop it and Fabric API into `mods/`.
3. If versions have moved on, update `gradle.properties` from https://fabricmc.net/develop.

## In progress: main menu (uncompiled)
`SnowMenuScreen` + `ui/Draw` port `preview/main-menu.html` using only GUI calls confirmed in the Fabric 26.x
docs (`GuiGraphicsExtractor.fill/outline/text`, `Button.builder`, `gui.setScreen`). Never compiled. Lines marked
`CHECK` in the source are the likeliest first-build errors. Profiles and SNOW Vision buttons are disabled
until those screens exist. Not done: real blur (translucent fallback only), distressed logo font,
button stagger, custom glass buttons, parallax, theme settings screen.

## Not implemented yet
Module menu, HUD + editor, crosshair editor, profiles,
keybind manager, session dashboard, performance panel, mod compatibility page, SNOW Vision
(WASAPI helper, visualizer, activity monitor), and notification/search UI. The logic behind
animation, theme, layout, config, notifications and search is done and tested, but nothing
renders it yet. Rendering code must be written against 26.3's actual GUI API (26.1 renamed
`GuiGraphics` to `GuiGraphicsExtractor`), which needs a machine that can resolve Minecraft sources.
