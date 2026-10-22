package gg.overthisnow.snow.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Versioned JSON config file with safe loading and saving.
 *
 * <ul>
 *   <li>Writes are atomic (temp file + move) so a crash cannot truncate the config.</li>
 *   <li>A malformed file is never overwritten silently: it is renamed to
 *       {@code name.corrupt-<timestamp>} and defaults are used.</li>
 *   <li>A newer schema than this build understands is left untouched on disk (read-only
 *       fallback to defaults) so downgrading cannot destroy user data.</li>
 * </ul>
 */
public final class ConfigStore<T> {
    public static final String VERSION_KEY = "schemaVersion";

    /** Outcome of a load, so the UI can honestly tell the user what happened. */
    public enum LoadStatus { LOADED, CREATED_DEFAULTS, RECOVERED_FROM_CORRUPT, NEWER_SCHEMA_READ_ONLY, MIGRATED }

    public record LoadResult<T>(T value, LoadStatus status, String detail) {}

    /** Migrates a raw JSON object from version {@code from} to {@code from + 1}. */
    @FunctionalInterface
    public interface Migration { void migrate(JsonObject root); }

    private final Path file;
    private final Class<T> type;
    private final int currentVersion;
    private final Migration[] migrations; // migrations[i] upgrades version i+1 -> i+2
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private boolean readOnly;

    public ConfigStore(Path file, Class<T> type, int currentVersion, Migration... migrations) {
        if (migrations.length != Math.max(0, currentVersion - 1)) {
            throw new IllegalArgumentException("Need exactly " + (currentVersion - 1) + " migrations");
        }
        this.file = file;
        this.type = type;
        this.currentVersion = currentVersion;
        this.migrations = migrations;
    }

    public LoadResult<T> load(java.util.function.Supplier<T> defaults, Consumer<T> validate) {
        if (!Files.exists(file)) {
            T d = defaults.get();
            validate.accept(d);
            return new LoadResult<>(d, LoadStatus.CREATED_DEFAULTS, "No config yet");
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new LoadResult<>(defaultsValidated(defaults, validate), LoadStatus.CREATED_DEFAULTS,
                    "Could not read config: " + e.getMessage());
        }
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) throw new IllegalStateException("root is not an object");
            root = el.getAsJsonObject();
        } catch (RuntimeException e) {
            return recoverCorrupt(defaults, validate, e.getMessage());
        }

        int version = root.has(VERSION_KEY) && root.get(VERSION_KEY).isJsonPrimitive()
                ? safeInt(root.get(VERSION_KEY)) : 1;
        if (version > currentVersion) {
            readOnly = true;
            return new LoadResult<>(defaultsValidated(defaults, validate), LoadStatus.NEWER_SCHEMA_READ_ONLY,
                    "Config schema v" + version + " is newer than supported v" + currentVersion
                            + "; file left untouched and will not be overwritten");
        }
        boolean migrated = false;
        try {
            while (version < currentVersion) {
                migrations[version - 1].migrate(root);
                version++;
                migrated = true;
            }
            T value = gson.fromJson(root, type);
            if (value == null) throw new IllegalStateException("empty config");
            validate.accept(value);
            return new LoadResult<>(value, migrated ? LoadStatus.MIGRATED : LoadStatus.LOADED,
                    migrated ? "Migrated to v" + currentVersion : "OK");
        } catch (RuntimeException e) {
            return recoverCorrupt(defaults, validate, e.getMessage());
        }
    }

    /** Atomically persists the value. Returns false (and does nothing) when read-only. */
    public boolean save(T value) throws IOException {
        if (readOnly) return false;
        JsonObject root = gson.toJsonTree(value).getAsJsonObject();
        root.addProperty(VERSION_KEY, currentVersion);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, gson.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private LoadResult<T> recoverCorrupt(java.util.function.Supplier<T> defaults, Consumer<T> validate, String why) {
        String detail = "Config was malformed (" + why + ")";
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            detail += "; original kept as " + backup.getFileName();
        } catch (IOException io) {
            // Could not back it up: refuse to overwrite it later.
            readOnly = true;
            detail += "; could not back it up, so it will not be overwritten";
        }
        return new LoadResult<>(defaultsValidated(defaults, validate), LoadStatus.RECOVERED_FROM_CORRUPT, detail);
    }

    private T defaultsValidated(java.util.function.Supplier<T> defaults, Consumer<T> validate) {
        T d = defaults.get();
        validate.accept(d);
        return d;
    }

    private static int safeInt(JsonElement e) {
        try { return e.getAsInt(); } catch (RuntimeException ex) { return 1; }
    }

    public boolean isReadOnly() { return readOnly; }
}
