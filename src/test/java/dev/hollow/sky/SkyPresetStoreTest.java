package dev.hollow.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkyPresetStoreTest {
	private final List<String> log = new ArrayList<>();

	private static JsonObject values(String zenith) {
		JsonObject values = new JsonObject();
		values.addProperty("zenith", zenith);
		return values;
	}

	private SkyPresetStore store(Path dir) {
		return new SkyPresetStore(dir, List.of(new SkyPresetStore.Preset("Void Realm", "Purple nebulae", true, values("#FF000000"))), log::add);
	}

	@Test
	void savesAndReloadsCustomPresets(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		store.save("My Night", "test", values("#FF101020"));
		SkyPresetStore reloaded = store(dir);
		reloaded.load();
		assertEquals("#FF101020", reloaded.find("my night").orElseThrow().values().get("zenith").getAsString());
		assertTrue(Files.exists(dir.resolve("my_night.json")));
	}

	@Test
	void builtInsCannotBeOverwrittenOrRenamed(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		assertThrows(IllegalArgumentException.class, () -> store.save("void realm", "", values("#FF000000")));
		assertThrows(IllegalArgumentException.class, () -> store.rename("Void Realm", "Other"));
	}

	@Test
	void duplicateRenameAndDelete(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		SkyPresetStore.Preset copy = store.duplicate("Void Realm");
		assertEquals("Void Realm Copy", copy.name());
		assertEquals("Void Realm Copy 2", store.duplicate("Void Realm").name());
		SkyPresetStore.Preset renamed = store.rename("Void Realm Copy", "Deep Void");
		assertEquals("Deep Void", renamed.name());
		assertFalse(Files.exists(dir.resolve("void_realm_copy.json")));
		assertTrue(store.delete("deep void"));
		assertFalse(Files.exists(dir.resolve("deep_void.json")));
	}

	@Test
	void exportImportRoundTripAvoidsNameClashes(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		SkyPresetStore.Preset original = store.save("Crimson", "red", values("#FFAA0000"));
		String json = store.export(original);
		SkyPresetStore.Preset imported = store.importJson(json);
		assertEquals("Crimson 2", imported.name());
		assertEquals("#FFAA0000", imported.values().get("zenith").getAsString());
	}

	@Test
	void rejectsForeignOrBrokenData(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		assertThrows(IllegalArgumentException.class, () -> store.importJson("{\"name\":\"x\",\"values\":{}}"));
		assertThrows(IllegalArgumentException.class, () -> store.importJson("not json {"));
		assertThrows(IllegalArgumentException.class, () -> store.importJson("x".repeat(SkyPresetStore.MAX_IMPORT_BYTES + 1)));
	}

	@Test
	void brokenFilesAreSkippedOnLoad(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("broken.json"), "{ nope");
		SkyPresetStore store = store(dir);
		store.load();
		assertTrue(store.customPresets().isEmpty());
		assertFalse(log.isEmpty());
	}

	@Test
	void namesAreSanitized() {
		assertEquals("Night Sky", SkyPresetStore.sanitizeName("  Night\tSky<>/ "));
		assertEquals(SkyPresetStore.MAX_NAME_LENGTH, SkyPresetStore.sanitizeName("a".repeat(60)).length());
		assertThrows(IllegalArgumentException.class, () -> SkyPresetStore.sanitizeName("<>"));
	}

	@Test
	void searchMatchesNameAndDescription(@TempDir Path dir) {
		SkyPresetStore store = store(dir);
		store.save("Ember", "warm dusk", values("#FF000000"));
		assertEquals(1, store.search("nebula").size());
		assertEquals(1, store.search("DUSK").size());
		assertEquals(2, store.search("").size());
	}
}
