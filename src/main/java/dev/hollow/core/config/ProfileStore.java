package dev.hollow.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Named snapshots of the whole configuration, stored as one JSON file each in
 * {@code config/hollow/profiles}. Loading a profile applies it through {@link ConfigManager},
 * so profiles from older versions are migrated like the main config.
 */
public final class ProfileStore {
	public static final String FORMAT = "holo-profile";
	public static final int MAX_NAME_LENGTH = 32;
	public static final int MAX_IMPORT_BYTES = 512 * 1024;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public record Profile(String name, long savedAt) {
	}

	private final Path directory;
	private final ConfigManager config;
	private final Consumer<String> log;

	public ProfileStore(Path directory, ConfigManager config, Consumer<String> log) {
		this.directory = directory;
		this.config = config;
		this.log = log;
	}

	public Path directory() {
		return directory;
	}

	/** Profiles sorted by name. Unreadable files are skipped and logged. */
	public List<Profile> list() {
		List<Profile> profiles = new ArrayList<>();
		if (!Files.isDirectory(directory)) {
			return profiles;
		}
		try (Stream<Path> files = Files.list(directory)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
				try {
					JsonObject root = read(Files.readString(file, StandardCharsets.UTF_8));
					profiles.add(new Profile(root.get("name").getAsString(), root.has("saved") ? root.get("saved").getAsLong() : 0));
				} catch (IOException | RuntimeException e) {
					log.accept("Skipping unreadable profile " + file.getFileName() + ": " + e.getMessage());
				}
			}
		} catch (IOException e) {
			log.accept("Could not list profiles: " + e.getMessage());
		}
		profiles.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return profiles;
	}

	public boolean exists(String name) {
		return list().stream().anyMatch(p -> p.name().equalsIgnoreCase(name));
	}

	/** Saves the current configuration under a name, replacing a profile with the same name. */
	public String saveCurrent(String requestedName, long now) throws IOException {
		String name = sanitizeName(requestedName);
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Enter a profile name");
		}
		JsonObject root = new JsonObject();
		root.addProperty("format", FORMAT);
		root.addProperty("version", 1);
		root.addProperty("name", name);
		root.addProperty("saved", now);
		JsonObject snapshot = config.toJson();
		snapshot.remove("profile");
		root.add("config", snapshot);
		Files.createDirectories(directory);
		Path existing = fileOf(name);
		Files.writeString(existing != null ? existing : freeFile(name), GSON.toJson(root), StandardCharsets.UTF_8);
		config.activeProfile(name);
		return name;
	}

	/** Applies a saved profile to every module. */
	public void load(String name) throws IOException {
		Path file = fileOf(name);
		if (file == null) {
			throw new IllegalArgumentException("No profile named " + name);
		}
		JsonObject root = read(Files.readString(file, StandardCharsets.UTF_8));
		config.apply(root.getAsJsonObject("config"));
		config.activeProfile(root.get("name").getAsString());
	}

	public void delete(String name) throws IOException {
		Path file = fileOf(name);
		if (file == null) {
			throw new IllegalArgumentException("No profile named " + name);
		}
		Files.delete(file);
		if (config.activeProfile().equalsIgnoreCase(name)) {
			config.activeProfile("");
		}
	}

	public String rename(String from, String to) throws IOException {
		Path file = fileOf(from);
		if (file == null) {
			throw new IllegalArgumentException("No profile named " + from);
		}
		String name = sanitizeName(to);
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Enter a new name");
		}
		if (!name.equalsIgnoreCase(from) && exists(name)) {
			throw new IllegalArgumentException("A profile named " + name + " already exists");
		}
		JsonObject root = read(Files.readString(file, StandardCharsets.UTF_8));
		root.addProperty("name", name);
		Files.delete(file);
		Files.writeString(freeFile(name), GSON.toJson(root), StandardCharsets.UTF_8);
		if (config.activeProfile().equalsIgnoreCase(from)) {
			config.activeProfile(name);
		}
		return name;
	}

	/** The profile file's JSON, for sharing. */
	public String export(String name) throws IOException {
		Path file = fileOf(name);
		if (file == null) {
			throw new IllegalArgumentException("No profile named " + name);
		}
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	/** Validates and stores a shared profile; a name clash gets a numbered copy. Returns the stored name. */
	public String importJson(String json) throws IOException {
		if (json == null || json.isBlank()) {
			throw new IllegalArgumentException("Nothing to import");
		}
		if (json.getBytes(StandardCharsets.UTF_8).length > MAX_IMPORT_BYTES) {
			throw new IllegalArgumentException("Profile is too large");
		}
		JsonObject root = read(json);
		String name = uniqueName(sanitizeName(root.get("name").getAsString()));
		root.addProperty("name", name);
		Files.createDirectories(directory);
		Files.writeString(freeFile(name), GSON.toJson(root), StandardCharsets.UTF_8);
		return name;
	}

	private String uniqueName(String base) {
		String name = base.isEmpty() ? "Imported" : base;
		if (!exists(name)) {
			return name;
		}
		for (int i = 2; ; i++) {
			String suffix = " (" + i + ")";
			String candidate = name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH - suffix.length())) + suffix;
			if (!exists(candidate)) {
				return candidate;
			}
		}
	}

	/** A file name for a new profile that doesn't overwrite another profile whose name has the same slug. */
	private Path freeFile(String name) {
		Path file = directory.resolve(slug(name) + ".json");
		for (int i = 2; Files.exists(file); i++) {
			file = directory.resolve(slug(name) + "_" + i + ".json");
		}
		return file;
	}

	private Path fileOf(String name) {
		if (!Files.isDirectory(directory)) {
			return null;
		}
		try (Stream<Path> files = Files.list(directory)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
				try {
					JsonObject root = read(Files.readString(file, StandardCharsets.UTF_8));
					if (root.get("name").getAsString().equalsIgnoreCase(name)) {
						return file;
					}
				} catch (IOException | RuntimeException e) {
					// Unreadable files are reported by list().
				}
			}
		} catch (IOException e) {
			return null;
		}
		return null;
	}

	/** Parses and validates a profile document. */
	static JsonObject read(String json) {
		JsonElement parsed;
		try {
			parsed = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("Not valid JSON");
		}
		if (!parsed.isJsonObject()) {
			throw new IllegalArgumentException("Not a Holo profile");
		}
		JsonObject root = parsed.getAsJsonObject();
		if (!root.has("format") || !FORMAT.equals(root.get("format").getAsString())) {
			throw new IllegalArgumentException("Not a Holo profile");
		}
		if (!root.has("name") || !root.get("name").isJsonPrimitive()) {
			throw new IllegalArgumentException("Profile has no name");
		}
		if (!root.has("config") || !root.get("config").isJsonObject() || !root.getAsJsonObject("config").has("modules")) {
			throw new IllegalArgumentException("Profile has no settings");
		}
		return root;
	}

	public static String sanitizeName(String name) {
		if (name == null) {
			return "";
		}
		String clean = name.replaceAll("\\s+", " ").replaceAll("[^\\p{L}\\p{N} _'().!&+-]", "").trim();
		return clean.length() > MAX_NAME_LENGTH ? clean.substring(0, MAX_NAME_LENGTH).trim() : clean;
	}

	static String slug(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return slug.isEmpty() ? "profile" : slug;
	}
}
