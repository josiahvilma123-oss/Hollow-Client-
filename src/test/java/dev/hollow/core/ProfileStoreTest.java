package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.config.ConfigManager;
import dev.hollow.core.config.ProfileStore;
import dev.hollow.core.input.Keybind;
import dev.hollow.core.module.ModuleManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTest {
	private final List<String> log = new ArrayList<>();

	private record Setup(ModuleManager modules, ConfigManagerTest.TestModule module, ConfigManager config, ProfileStore profiles) {
	}

	private Setup setup(Path dir) {
		ModuleManager modules = new ModuleManager();
		ConfigManagerTest.TestModule module = modules.register(new ConfigManagerTest.TestModule("finder", false));
		ConfigManager config = new ConfigManager(dir.resolve("hollow.json"), modules, () -> 0, 0, log::add);
		config.load();
		return new Setup(modules, module, config, new ProfileStore(dir.resolve("profiles"), config, log::add));
	}

	@Test
	void saveLoadRestoresEverything(@TempDir Path dir) throws IOException {
		Setup s = setup(dir);
		s.module().setEnabled(true);
		s.module().range.set(100.0);
		s.module().keybind().set(Keybind.key(71, Keybind.CTRL));
		assertEquals("PvP", s.profiles().saveCurrent("PvP", 5));
		assertEquals("PvP", s.config().activeProfile());

		s.module().setEnabled(false);
		s.module().range.set(16.0);
		s.module().keybind().set(Keybind.NONE);
		s.profiles().load("pvp");
		assertTrue(s.module().isEnabled());
		assertEquals(100.0, s.module().range.get());
		assertEquals(Keybind.key(71, Keybind.CTRL), s.module().keybind().get());
	}

	@Test
	void loadingResetsSettingsMissingFromTheProfile(@TempDir Path dir) throws IOException {
		Setup s = setup(dir);
		s.profiles().saveCurrent("Defaults", 0);
		s.module().labels.set(false);
		s.profiles().load("Defaults");
		assertTrue(s.module().labels.get(), "a profile is a whole setup, not a patch");
	}

	@Test
	void renameExportImportDelete(@TempDir Path dir) throws IOException {
		Setup s = setup(dir);
		s.profiles().saveCurrent("Bridging", 0);
		assertEquals("Speed", s.profiles().rename("Bridging", "Speed"));
		assertFalse(s.profiles().exists("Bridging"));
		assertEquals("Speed", s.profiles().list().get(0).name());

		String shared = s.profiles().export("Speed");
		assertEquals("Speed (2)", s.profiles().importJson(shared), "a name clash gets a numbered copy");
		assertEquals(2, s.profiles().list().size());

		s.profiles().delete("Speed");
		assertEquals(List.of("Speed (2)"), s.profiles().list().stream().map(ProfileStore.Profile::name).toList());
		assertThrows(IllegalArgumentException.class, () -> s.profiles().load("Speed"));
	}

	@Test
	void namesWhoseFilesWouldCollideStaySeparate(@TempDir Path dir) throws IOException {
		Setup s = setup(dir);
		s.module().range.set(40.0);
		s.profiles().saveCurrent("My Setup", 0);
		s.module().range.set(80.0);
		s.profiles().saveCurrent("My-Setup", 0);
		assertEquals(2, s.profiles().list().size());
		s.profiles().load("My Setup");
		assertEquals(40.0, s.module().range.get());
	}

	@Test
	void rejectsInvalidInput(@TempDir Path dir) {
		Setup s = setup(dir);
		assertThrows(IllegalArgumentException.class, () -> s.profiles().saveCurrent("   ", 0));
		assertThrows(IllegalArgumentException.class, () -> s.profiles().importJson("{\"format\":\"holo-profile\",\"name\":\"x\"}"));
		assertThrows(IllegalArgumentException.class, () -> s.profiles().importJson("[]"));
		assertThrows(IllegalArgumentException.class, () -> s.profiles().importJson("x".repeat(ProfileStore.MAX_IMPORT_BYTES + 1)));
		assertEquals("Ok Name", ProfileStore.sanitizeName("  Ok\u0000 \n Name/ "));
		assertEquals(ProfileStore.MAX_NAME_LENGTH, ProfileStore.sanitizeName("a".repeat(100)).length());
	}

	@Test
	void olderProfilesAreMigratedOnLoad(@TempDir Path dir) throws IOException {
		Setup s = setup(dir);
		String v1 = """
			{"format":"holo-profile","version":1,"name":"Old","saved":0,
			 "config":{"version":1,"modules":{"finder":{"enabled":true,"settings":{"range":50,"keybind":72}}}}}""";
		assertEquals("Old", s.profiles().importJson(v1));
		s.profiles().load("Old");
		assertTrue(s.module().isEnabled());
		assertEquals(50.0, s.module().range.get());
		assertEquals(Keybind.key(72), s.module().keybind().get(), "the old numeric keybind still reads");
	}
}
