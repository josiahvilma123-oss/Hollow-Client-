package dev.hollow.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Clock;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/**
 * Persists module state and settings as JSON. Writes are debounced and atomic, and a
 * damaged file is backed up instead of crashing the client.
 */
public final class ConfigManager {
	/**
	 * Config schema version. History:
	 * 1 - initial release (visuals, detection, alerts).
	 * 2 - Holo update: Sky Studio, Character Studio, theme packs, main menu, loading screen,
	 *     performance. Existing module settings are untouched; new modules start at defaults.
	 */
	public static final int VERSION = 2;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;
	private final ModuleManager modules;
	private final Clock clock;
	private final long debounceMillis;
	private final Consumer<String> log;
	private long dirtySince = -1;
	private boolean loading;

	public ConfigManager(Path file, ModuleManager modules, Clock clock, long debounceMillis, Consumer<String> log) {
		this.file = file;
		this.modules = modules;
		this.clock = clock;
		this.debounceMillis = debounceMillis;
		this.log = log;
	}

	public Path file() {
		return file;
	}

	/** Registers change listeners on every module and setting so edits are saved automatically. */
	public void watch() {
		for (Module module : modules.all()) {
			module.onStateChange(this::markDirty);
			for (Setting<?> setting : module.settings()) {
				if (setting.persistent()) {
					setting.onChange(value -> markDirty());
				}
			}
		}
	}

	public void markDirty() {
		if (loading) {
			return;
		}
		if (dirtySince < 0) {
			dirtySince = clock.millis();
		}
	}

	public boolean isDirty() {
		return dirtySince >= 0;
	}

	/** Call regularly (e.g. every client tick); saves once edits have settled. */
	public void tick() {
		if (dirtySince >= 0 && clock.millis() - dirtySince >= debounceMillis) {
			save();
		}
	}

	public void load() {
		if (!Files.isRegularFile(file)) {
			applyDefaults();
			return;
		}
		JsonObject root;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				throw new JsonParseException("root is not an object");
			}
			root = parsed.getAsJsonObject();
		} catch (IOException | JsonParseException | IllegalStateException e) {
			backupBrokenFile(e);
			applyDefaults();
			return;
		}
		int version = root.has("version") && root.get("version").isJsonPrimitive() ? root.get("version").getAsInt() : 1;
		if (version < VERSION) {
			backupBeforeMigration(version);
			root = ConfigMigrations.migrate(root, version, log);
		}
		loading = true;
		try {
			JsonObject moduleObjects = root.has("modules") && root.get("modules").isJsonObject()
				? root.getAsJsonObject("modules") : new JsonObject();
			for (Module module : modules.all()) {
				JsonElement element = moduleObjects.get(module.id());
				if (element == null || !element.isJsonObject()) {
					module.setEnabled(module.defaultEnabled());
					continue;
				}
				loadModule(module, element.getAsJsonObject());
			}
		} finally {
			loading = false;
		}
		dirtySince = version < VERSION ? clock.millis() : -1;
	}

	private void backupBeforeMigration(int version) {
		Path backup = file.resolveSibling(file.getFileName() + ".v" + version + ".bak");
		try {
			Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
			log.accept("Migrating config from version " + version + " to " + VERSION + "; previous file kept at " + backup);
		} catch (IOException e) {
			log.accept("Could not back up config before migration: " + e.getMessage());
		}
	}

	private void loadModule(Module module, JsonObject object) {
		JsonObject settings = object.has("settings") && object.get("settings").isJsonObject()
			? object.getAsJsonObject("settings") : new JsonObject();
		for (Setting<?> setting : module.settings()) {
			if (!setting.persistent() || !settings.has(setting.id())) {
				continue;
			}
			try {
				setting.fromJson(settings.get(setting.id()));
			} catch (RuntimeException e) {
				log.accept("Ignoring invalid value for " + module.id() + "." + setting.id() + ": " + e.getMessage());
			}
		}
		boolean enabled = module.defaultEnabled();
		JsonElement enabledElement = object.get("enabled");
		if (module.isToggleable() && enabledElement != null && enabledElement.isJsonPrimitive()
				&& enabledElement.getAsJsonPrimitive().isBoolean()) {
			enabled = enabledElement.getAsBoolean();
		}
		try {
			module.setEnabled(enabled);
		} catch (RuntimeException e) {
			log.accept("Could not restore state of " + module.id() + ": " + e.getMessage());
		}
	}

	private void applyDefaults() {
		loading = true;
		try {
			for (Module module : modules.all()) {
				module.setEnabled(module.defaultEnabled());
			}
		} finally {
			loading = false;
		}
		dirtySince = -1;
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		JsonObject moduleObjects = new JsonObject();
		for (Module module : modules.all()) {
			JsonObject object = new JsonObject();
			object.addProperty("enabled", module.isEnabled());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.settings()) {
				if (setting.persistent()) {
					settings.add(setting.id(), setting.toJson());
				}
			}
			object.add("settings", settings);
			moduleObjects.add(module.id(), object);
		}
		root.add("modules", moduleObjects);
		return root;
	}

	public void save() {
		dirtySince = -1;
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(toJson(), writer);
			}
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.accept("Failed to save config " + file + ": " + e.getMessage());
		}
	}

	private void backupBrokenFile(Exception cause) {
		Path backup = file.resolveSibling(file.getFileName() + ".broken");
		try {
			Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
			log.accept("Config was unreadable (" + cause.getMessage() + "); moved it to " + backup + " and restored defaults");
		} catch (IOException e) {
			log.accept("Config was unreadable and could not be backed up: " + e.getMessage());
		}
	}
}
