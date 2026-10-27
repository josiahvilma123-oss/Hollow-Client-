package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hollow.core.config.ConfigManager;
import dev.hollow.core.config.ConfigMigrations;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerTest {
	static final class TestModule extends Module {
		final NumberSetting range = add(new NumberSetting("range", "Range", "", 32, 8, 128, 1));
		final BoolSetting labels = add(new BoolSetting("labels", "Labels", "", true));
		int enables;
		int disables;

		TestModule(String id, boolean defaultEnabled) {
			super(id, id, "test", Category.RENDER, defaultEnabled);
		}

		@Override
		protected void onEnable() {
			enables++;
		}

		@Override
		protected void onDisable() {
			disables++;
		}
	}

	private final List<String> log = new ArrayList<>();

	@Test
	void savesAndRestoresModulesAndSettings(@TempDir Path dir) {
		Path file = dir.resolve("hollow.json");
		ModuleManager modules = new ModuleManager();
		TestModule finder = modules.register(new TestModule("finder", false));
		ConfigManager config = new ConfigManager(file, modules, () -> 0, 0, log::add);
		config.load();
		finder.setEnabled(true);
		finder.range.set(64.0);
		finder.labels.set(false);
		finder.keybind().set(dev.hollow.core.input.Keybind.key(71));
		config.save();

		ModuleManager reloadedModules = new ModuleManager();
		TestModule reloaded = reloadedModules.register(new TestModule("finder", false));
		new ConfigManager(file, reloadedModules, () -> 0, 0, log::add).load();
		assertTrue(reloaded.isEnabled());
		assertEquals(64.0, reloaded.range.get());
		assertFalse(reloaded.labels.get());
		assertEquals(dev.hollow.core.input.Keybind.key(71), reloaded.keybind().get());
		assertEquals(1, reloaded.enables);
	}

	@Test
	void missingFileAppliesDefaults(@TempDir Path dir) {
		ModuleManager modules = new ModuleManager();
		TestModule on = modules.register(new TestModule("on", true));
		TestModule off = modules.register(new TestModule("off", false));
		new ConfigManager(dir.resolve("none.json"), modules, () -> 0, 0, log::add).load();
		assertTrue(on.isEnabled());
		assertFalse(off.isEnabled());
	}

	@Test
	void corruptFileIsBackedUpAndDefaultsUsed(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("hollow.json");
		Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);
		ModuleManager modules = new ModuleManager();
		TestModule module = modules.register(new TestModule("m", true));
		new ConfigManager(file, modules, () -> 0, 0, log::add).load();
		assertTrue(module.isEnabled());
		assertTrue(Files.exists(dir.resolve("hollow.json.broken")));
		assertFalse(log.isEmpty());
	}

	@Test
	void invalidValuesAreIgnoredPerSetting(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("hollow.json");
		Files.writeString(file, """
			{"version":1,"modules":{"m":{"enabled":true,"settings":{"range":"far","labels":false}}}}
			""", StandardCharsets.UTF_8);
		ModuleManager modules = new ModuleManager();
		TestModule module = modules.register(new TestModule("m", false));
		new ConfigManager(file, modules, () -> 0, 0, log::add).load();
		assertEquals(32.0, module.range.get());
		assertFalse(module.labels.get());
		assertTrue(module.isEnabled());
	}

	@Test
	void savesAreDebounced(@TempDir Path dir) {
		Path file = dir.resolve("hollow.json");
		AtomicLong now = new AtomicLong(1000);
		ModuleManager modules = new ModuleManager();
		TestModule module = modules.register(new TestModule("m", false));
		ConfigManager config = new ConfigManager(file, modules, now::get, 750, log::add);
		config.load();
		config.watch();
		assertFalse(config.isDirty());
		module.range.set(40.0);
		assertTrue(config.isDirty());
		now.addAndGet(500);
		config.tick();
		assertFalse(Files.exists(file));
		now.addAndGet(300);
		config.tick();
		assertTrue(Files.exists(file));
		assertFalse(config.isDirty());
	}

	@Test
	void unavailableModuleCannotBeEnabled() {
		TestModule module = new TestModule("m", false);
		module.setEnabled(true);
		module.markUnavailable("render hook missing");
		assertFalse(module.isEnabled());
		assertEquals(1, module.disables);
		module.setEnabled(true);
		assertFalse(module.isEnabled());
		assertEquals("render hook missing", module.unavailableReason().orElseThrow());
	}

	@Test
	void moduleSearchIncludesSettings() {
		TestModule module = new TestModule("finder", false);
		assertTrue(module.matches("range"));
		assertTrue(module.matches("key mode"));
		assertFalse(module.matches("zzz"));
	}

	@Test
	void migratesVersionOneConfigKeepingValues(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("hollow.json");
		Files.writeString(file, """
			{"version":1,"modules":{"m":{"enabled":true,"settings":{"range":50}}}}
			""", StandardCharsets.UTF_8);
		ModuleManager modules = new ModuleManager();
		TestModule module = modules.register(new TestModule("m", false));
		TestModule added = modules.register(new TestModule("new_module", true));
		ConfigManager config = new ConfigManager(file, modules, () -> 0, 0, log::add);
		config.load();
		assertTrue(module.isEnabled());
		assertEquals(50.0, module.range.get());
		assertTrue(added.isEnabled());
		assertTrue(Files.exists(dir.resolve("hollow.json.v1.bak")));
		config.tick();
		assertTrue(Files.readString(file).contains("\"version\": 4"));
	}

	@Test
	void migratesNamedFeaturedCharacterToChooseSetting() {
		JsonObject v2 = JsonParser.parseString("""
			{"version":2,"modules":{"main_menu":{"enabled":true,"settings":{"character":"WRAITH","scene":"VOID"}}}}
			""").getAsJsonObject();
		JsonObject settings = ConfigMigrations.migrate(v2, 2, log::add)
			.getAsJsonObject("modules").getAsJsonObject("main_menu").getAsJsonObject("settings");
		assertEquals("CHOOSE", settings.get("character").getAsString());
		assertEquals("WRAITH", settings.get("featured_character").getAsString());
		assertEquals("VOID", settings.get("scene").getAsString());

		JsonObject following = JsonParser.parseString("""
			{"version":2,"modules":{"main_menu":{"settings":{"character":"FOLLOW_STUDIO"}}}}
			""").getAsJsonObject();
		JsonObject kept = ConfigMigrations.migrate(following, 2, log::add)
			.getAsJsonObject("modules").getAsJsonObject("main_menu").getAsJsonObject("settings");
		assertEquals("FOLLOW_STUDIO", kept.get("character").getAsString());
		assertFalse(kept.has("featured_character"));
	}

	@Test
	void migratesPerformanceProfileToPreset() {
		JsonObject v3 = JsonParser.parseString("""
			{"version":3,"modules":{"performance":{"enabled":true,"settings":{"profile":"QUALITY"}},
			 "motion_blur":{"enabled":true,"settings":{"keybind":71}}}}
			""").getAsJsonObject();
		JsonObject migrated = ConfigMigrations.migrate(v3, 3, log::add);
		JsonObject performance = migrated.getAsJsonObject("modules").getAsJsonObject("performance").getAsJsonObject("settings");
		assertEquals("HIGH_QUALITY", performance.get("preset").getAsString());
		assertFalse(performance.has("profile"));
		assertEquals(71, migrated.getAsJsonObject("modules").getAsJsonObject("motion_blur").getAsJsonObject("settings").get("keybind").getAsInt(),
			"unrelated settings, including keybinds, are untouched");
		assertEquals(4, migrated.get("version").getAsInt());
		for (String old : List.of("LOW", "BALANCED")) {
			JsonObject root = JsonParser.parseString("{\"modules\":{\"performance\":{\"settings\":{\"profile\":\"" + old + "\"}}}}")
				.getAsJsonObject();
			assertEquals(old, ConfigMigrations.migrate(root, 3, log::add).getAsJsonObject("modules").getAsJsonObject("performance")
				.getAsJsonObject("settings").get("preset").getAsString());
		}
	}

	@Test
	void interfaceThemesSetColorsAndCustomColorsSwitchToCustom(@TempDir Path dir) throws IOException {
		dev.hollow.modules.InterfaceModule ui = new dev.hollow.modules.InterfaceModule();
		ui.theme.set(dev.hollow.modules.InterfaceModule.ThemePreset.CRIMSON);
		assertEquals(dev.hollow.modules.InterfaceModule.ThemePreset.CRIMSON.accent(), ui.accent.get());
		ui.accent.set(0xFF123456);
		assertEquals(dev.hollow.modules.InterfaceModule.ThemePreset.CUSTOM, ui.theme.get());

		// A pre-1.3 config with a hand-picked accent and no theme keeps that accent as a Custom theme.
		Path file = dir.resolve("hollow.json");
		Files.writeString(file, """
			{"version":3,"modules":{"interface":{"enabled":true,"settings":{"accent":"#FF2FBF71","panel_opacity":0.9}}}}
			""");
		ModuleManager modules = new ModuleManager();
		dev.hollow.modules.InterfaceModule loaded = modules.register(new dev.hollow.modules.InterfaceModule());
		new ConfigManager(file, modules, () -> 0, 0, log::add).load();
		assertEquals(0xFF2FBF71, loaded.accent.get());
		assertEquals(dev.hollow.modules.InterfaceModule.ThemePreset.CUSTOM, loaded.theme.get());
	}
}
