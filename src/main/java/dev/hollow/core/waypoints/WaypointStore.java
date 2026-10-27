package dev.hollow.core.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Waypoints of one world (single-player save or server), stored in
 * {@code config/hollow/waypoints/<world>.json}. Writes are atomic; a damaged file is kept aside.
 */
public final class WaypointStore {
	public static final String FORMAT = "holo-waypoints";
	public static final int MAX_WAYPOINTS = 1000;
	public static final int MAX_IMPORT_BYTES = 1024 * 1024;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path directory;
	private final Consumer<String> log;
	private final List<Waypoint> waypoints = new ArrayList<>();
	private String world = "";
	private int revision;

	public WaypointStore(Path directory, Consumer<String> log) {
		this.directory = directory;
		this.log = log;
	}

	public Path directory() {
		return directory;
	}

	public String world() {
		return world;
	}

	/** Increments on every change, so views can cache. */
	public int revision() {
		return revision;
	}

	public List<Waypoint> all() {
		return Collections.unmodifiableList(waypoints);
	}

	/** Switches to a world's waypoints (an empty key clears the list, e.g. when disconnected). */
	public void open(String worldKey) {
		world = worldKey == null ? "" : worldKey;
		waypoints.clear();
		revision++;
		if (world.isEmpty()) {
			return;
		}
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			waypoints.addAll(parse(Files.readString(file, StandardCharsets.UTF_8)));
		} catch (IOException | IllegalArgumentException e) {
			log.accept("Waypoints for " + world + " were unreadable (" + e.getMessage() + "); keeping the file as .broken");
			try {
				Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// Nothing more to do; the list starts empty.
			}
		}
	}

	public Waypoint add(String name, int x, int y, int z, String dimension, Waypoint.Category category, String note, long now) {
		if (world.isEmpty()) {
			throw new IllegalStateException("Join a world first");
		}
		if (waypoints.size() >= MAX_WAYPOINTS) {
			throw new IllegalStateException("This world already has " + MAX_WAYPOINTS + " waypoints");
		}
		Waypoint waypoint = new Waypoint(UUID.randomUUID().toString(), name, x, y, z, dimension, category, 0, note, now, true);
		waypoints.add(waypoint);
		changed();
		return waypoint;
	}

	public void update(Waypoint waypoint) {
		for (int i = 0; i < waypoints.size(); i++) {
			if (waypoints.get(i).id().equals(waypoint.id())) {
				waypoints.set(i, waypoint);
				changed();
				return;
			}
		}
	}

	public boolean remove(String id) {
		boolean removed = waypoints.removeIf(w -> w.id().equals(id));
		if (removed) {
			changed();
		}
		return removed;
	}

	/** A name not yet used in this world, e.g. "Waypoint 3". */
	public String nextName(String base) {
		for (int i = 1; ; i++) {
			String candidate = base + " " + i;
			if (waypoints.stream().noneMatch(w -> w.name().equalsIgnoreCase(candidate))) {
				return candidate;
			}
		}
	}

	public String export() {
		JsonObject root = new JsonObject();
		root.addProperty("format", FORMAT);
		root.addProperty("version", 1);
		JsonArray array = new JsonArray();
		for (Waypoint waypoint : waypoints) {
			array.add(waypoint.toJson());
		}
		root.add("waypoints", array);
		return GSON.toJson(root);
	}

	/** Adds the waypoints from an export, skipping ones already present. Returns how many were added. */
	public int importJson(String json) {
		if (world.isEmpty()) {
			throw new IllegalStateException("Join a world first");
		}
		if (json == null || json.isBlank()) {
			throw new IllegalArgumentException("Nothing to import");
		}
		if (json.getBytes(StandardCharsets.UTF_8).length > MAX_IMPORT_BYTES) {
			throw new IllegalArgumentException("Import is too large");
		}
		int added = 0;
		for (Waypoint waypoint : parse(json)) {
			if (waypoints.size() >= MAX_WAYPOINTS) {
				break;
			}
			boolean duplicate = waypoints.stream().anyMatch(w -> w.id().equals(waypoint.id())
				|| w.x() == waypoint.x() && w.y() == waypoint.y() && w.z() == waypoint.z() && w.dimension().equals(waypoint.dimension()));
			if (!duplicate) {
				waypoints.add(waypoint);
				added++;
			}
		}
		if (added > 0) {
			changed();
		}
		return added;
	}

	static List<Waypoint> parse(String json) {
		JsonElement parsed;
		try {
			parsed = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("Not valid JSON");
		}
		if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("format")
				|| !FORMAT.equals(parsed.getAsJsonObject().get("format").getAsString())) {
			throw new IllegalArgumentException("Not a Holo waypoint export");
		}
		JsonElement array = parsed.getAsJsonObject().get("waypoints");
		if (array == null || !array.isJsonArray()) {
			throw new IllegalArgumentException("No waypoints in the export");
		}
		List<Waypoint> result = new ArrayList<>();
		for (JsonElement element : array.getAsJsonArray()) {
			if (element.isJsonObject()) {
				result.add(Waypoint.fromJson(element.getAsJsonObject()));
			}
		}
		return result;
	}

	private void changed() {
		revision++;
		save();
	}

	private void save() {
		if (world.isEmpty()) {
			return;
		}
		try {
			Files.createDirectories(directory);
			Path file = file();
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, export(), StandardCharsets.UTF_8);
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.accept("Could not save waypoints: " + e.getMessage());
		}
	}

	private Path file() {
		return directory.resolve(slug(world) + ".json");
	}

	public static String slug(String key) {
		String slug = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
		return slug.isEmpty() ? "world" : slug.length() > 80 ? slug.substring(0, 80) : slug;
	}
}
