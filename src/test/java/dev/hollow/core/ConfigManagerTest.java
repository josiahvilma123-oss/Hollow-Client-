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
			super(id, id, "test", Category.VISUALS_DETECTION, defaultEnabled);
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
		finder.keybind().set(71);
		config.save();

		ModuleManager reloadedModules = new ModuleManager();
		TestModule reloaded = reloadedModules.register(new TestModule("finder", false));
		new ConfigManager(file, reloadedModules, () -> 0, 0, log::add).load();
		assertTrue(reloaded.isEnabled());
		assertEquals(64.0, reloaded.range.get());
		assertFalse(reloaded.labels.get());
		assertEquals(71, reloaded.keybind().get());
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
		assertTrue(module.matches("toggle key"));
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
		assertTrue(Files.readString(file).contains("\"version\": 3"));
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
}
