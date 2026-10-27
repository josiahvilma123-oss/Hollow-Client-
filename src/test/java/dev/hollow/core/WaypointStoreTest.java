package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.core.waypoints.WaypointStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WaypointStoreTest {
	private final List<String> log = new ArrayList<>();

	@Test
	void waypointsArePerWorldAndPersist(@TempDir Path dir) {
		WaypointStore store = new WaypointStore(dir, log::add);
		store.open("sp_My World");
		store.add("Home", 10, 64, -20, "minecraft:overworld", Waypoint.Category.BASE, "Main base", 1000);
		store.open("mp_play.example.net");
		assertTrue(store.all().isEmpty(), "another world has its own list");
		store.add("Spawn", 0, 70, 0, "minecraft:overworld", Waypoint.Category.POI, "", 2000);

		WaypointStore reloaded = new WaypointStore(dir, log::add);
		reloaded.open("sp_My World");
		assertEquals(1, reloaded.all().size());
		Waypoint home = reloaded.all().get(0);
		assertEquals("Home", home.name());
		assertEquals(-20, home.z());
		assertEquals("Main base", home.note());
		assertEquals(1000, home.created());
		assertEquals(Waypoint.Category.BASE, home.category());
	}

	@Test
	void updateRemoveAndNames(@TempDir Path dir) {
		WaypointStore store = new WaypointStore(dir, log::add);
		store.open("sp_test");
		Waypoint a = store.add("Waypoint 1", 1, 2, 3, "minecraft:overworld", Waypoint.Category.SCOUTED, "", 0);
		assertEquals("Waypoint 2", store.nextName("Waypoint"));
		int revision = store.revision();
		store.update(a.withVisible(false));
		assertFalse(store.all().get(0).visible());
		assertTrue(store.revision() > revision);
		assertTrue(store.remove(a.id()));
		assertFalse(store.remove(a.id()));
		assertTrue(store.all().isEmpty());
	}

	@Test
	void importSkipsDuplicatesAndValidates(@TempDir Path dir) {
		WaypointStore store = new WaypointStore(dir, log::add);
		store.open("sp_a");
		store.add("Base", 5, 64, 5, "minecraft:overworld", Waypoint.Category.BASE, "", 0);
		store.add("Nether hub", 0, 70, 0, "minecraft:the_nether", Waypoint.Category.POI, "", 0);
		String export = store.export();

		store.open("sp_b");
		assertEquals(2, store.importJson(export));
		assertEquals(0, store.importJson(export), "importing the same export twice adds nothing");
		assertThrows(IllegalArgumentException.class, () -> store.importJson("{\"format\":\"something-else\"}"));
		assertThrows(IllegalArgumentException.class, () -> store.importJson("not json"));
		assertThrows(IllegalArgumentException.class, () -> store.importJson(""));
		store.open("");
		assertThrows(IllegalStateException.class, () -> store.importJson(export), "no world, nothing to import into");
	}

	@Test
	void valuesAreSanitised() {
		Waypoint w = new Waypoint("id", "  a\tvery\nlong name that goes on and on past the limit  ", 99_999_999, 99_999, 0, "  Minecraft:Overworld ",
			null, 0, null, 0, true);
		assertTrue(w.name().length() <= Waypoint.MAX_NAME);
		assertTrue(w.name().startsWith("a very long name"), "control characters become single spaces");
		assertFalse(w.name().contains("\n"));
		assertEquals(Waypoint.MAX_HORIZONTAL, w.x());
		assertEquals(Waypoint.MAX_VERTICAL, w.y());
		assertEquals("minecraft:overworld", w.dimension());
		assertEquals(Waypoint.Category.OTHER, w.category());
		assertEquals("", w.note());
		assertEquals("Waypoint", new Waypoint("id", "   ", 0, 0, 0, null, null, 0, "", 0, true).name());
	}

	@Test
	void unreadableFileIsKeptAsBroken(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve(WaypointStore.slug("sp_x") + ".json"), "{oops");
		WaypointStore store = new WaypointStore(dir, log::add);
		store.open("sp_x");
		assertTrue(store.all().isEmpty());
		assertTrue(Files.exists(dir.resolve(WaypointStore.slug("sp_x") + ".json.broken")));
		assertEquals(1, log.size());
	}
}
