package dev.hollow.sky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Library of sky presets: read-only built-ins plus user presets stored one JSON file each.
 * A preset is a name plus the Sky Studio setting values it applies.
 */
public final class SkyPresetStore {
	public static final String FORMAT = "holo-sky";
	public static final int FORMAT_VERSION = 1;
	public static final int MAX_NAME_LENGTH = 32;
	public static final int MAX_IMPORT_BYTES = 64 * 1024;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public record Preset(String name, String description, boolean builtIn, JsonObject values) {
		public Preset {
			values = values.deepCopy();
		}
	}

	private final Path directory;
	private final Consumer<String> log;
	private final List<Preset> builtIns = new ArrayList<>();
	private final Map<String, Preset> custom = new LinkedHashMap<>();

	public SkyPresetStore(Path directory, List<Preset> builtIns, Consumer<String> log) {
		this.directory = directory;
		this.log = log;
		this.builtIns.addAll(builtIns);
	}

	public Path directory() {
		return directory;
	}

	public List<Preset> builtIns() {
		return Collections.unmodifiableList(builtIns);
	}

	public List<Preset> customPresets() {
		return List.copyOf(custom.values());
	}

	public List<Preset> all() {
		List<Preset> all = new ArrayList<>(builtIns);
		all.addAll(custom.values());
		return all;
	}

	/** Presets whose name or description contains the query (case-insensitive). */
	public List<Preset> search(String query) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return all();
		}
		return all().stream()
			.filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q) || p.description().toLowerCase(Locale.ROOT).contains(q))
			.toList();
	}

	public Optional<Preset> find(String name) {
		if (name == null) {
			return Optional.empty();
		}
		for (Preset preset : builtIns) {
			if (preset.name().equalsIgnoreCase(name)) {
				return Optional.of(preset);
			}
		}
		return Optional.ofNullable(custom.get(key(name)));
	}

	public boolean isBuiltIn(String name) {
		return builtIns.stream().anyMatch(p -> p.name().equalsIgnoreCase(name));
	}

	/** Loads every preset file in the directory. Broken files are skipped with a log line. */
	public void load() {
		custom.clear();
		if (!Files.isDirectory(directory)) {
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
			for (Path file : files) {
				try {
					if (Files.size(file) > MAX_IMPORT_BYTES) {
						log.accept("Skipping oversized sky preset " + file.getFileName());
						continue;
					}
					Preset preset = parse(Files.readString(file, StandardCharsets.UTF_8));
					if (isBuiltIn(preset.name()) || custom.containsKey(key(preset.name()))) {
						preset = new Preset(uniqueName(preset.name()), preset.description(), false, preset.values());
					}
					custom.put(key(preset.name()), preset);
				} catch (IOException | IllegalArgumentException e) {
					log.accept("Skipping unreadable sky preset " + file.getFileName() + ": " + e.getMessage());
				}
			}
		} catch (IOException e) {
			log.accept("Could not list sky presets: " + e.getMessage());
		}
	}

	/** Creates or overwrites a custom preset and writes it to disk. Built-in names are refused. */
	public Preset save(String name, String description, JsonObject values) {
		String clean = sanitizeName(name);
		if (isBuiltIn(clean)) {
			throw new IllegalArgumentException("\"" + clean + "\" is a built-in preset; save under a new name");
		}
		Preset existing = custom.remove(key(clean));
		if (existing != null) {
			deleteFile(existing.name());
		}
		Preset preset = new Preset(clean, description == null ? "" : description, false, values);
		custom.put(key(clean), preset);
		write(preset);
		return preset;
	}

	public Preset rename(String from, String to) {
		Preset preset = custom.get(key(from));
		if (preset == null) {
			throw new IllegalArgumentException("Only custom presets can be renamed");
		}
		String clean = sanitizeName(to);
		if (!clean.equalsIgnoreCase(from) && (isBuiltIn(clean) || custom.containsKey(key(clean)))) {
			throw new IllegalArgumentException("A preset named \"" + clean + "\" already exists");
		}
		custom.remove(key(from));
		deleteFile(preset.name());
		Preset renamed = new Preset(clean, preset.description(), false, preset.values());
		custom.put(key(clean), renamed);
		write(renamed);
		return renamed;
	}

	/** Copies any preset (built-in or custom) into a new custom preset with a unique name. */
	public Preset duplicate(String name) {
		Preset source = find(name).orElseThrow(() -> new IllegalArgumentException("No preset named \"" + name + "\""));
		String copyName = uniqueName(source.name() + " Copy");
		return save(copyName, source.description(), source.values());
	}

	public boolean delete(String name) {
		Preset removed = custom.remove(key(name));
		if (removed == null) {
			return false;
		}
		deleteFile(removed.name());
		return true;
	}

	public String export(Preset preset) {
		JsonObject root = new JsonObject();
		root.addProperty("format", FORMAT);
		root.addProperty("version", FORMAT_VERSION);
		root.addProperty("name", preset.name());
		root.addProperty("description", preset.description());
		root.add("values", preset.values().deepCopy());
		return GSON.toJson(root);
	}

	/** Imports a preset from exported JSON, renaming it if the name is taken. */
	public Preset importJson(String json) {
		if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_IMPORT_BYTES) {
			throw new IllegalArgumentException("Preset data is empty or too large");
		}
		Preset parsed = parse(json);
		String name = isBuiltIn(parsed.name()) || custom.containsKey(key(parsed.name())) ? uniqueName(parsed.name()) : parsed.name();
		return save(name, parsed.description(), parsed.values());
	}

	public String uniqueName(String base) {
		String clean = sanitizeName(base);
		if (!isBuiltIn(clean) && !custom.containsKey(key(clean))) {
			return clean;
		}
		for (int i = 2; i < 1000; i++) {
			String suffix = " " + i;
			String stem = clean.length() + suffix.length() > MAX_NAME_LENGTH ? clean.substring(0, MAX_NAME_LENGTH - suffix.length()) : clean;
			String candidate = stem.trim() + suffix;
			if (!isBuiltIn(candidate) && !custom.containsKey(key(candidate))) {
				return candidate;
			}
		}
		throw new IllegalStateException("Too many presets named " + clean);
	}

	/** Keeps letters, digits, spaces and a few punctuation marks; trims to the length limit. */
	public static String sanitizeName(String name) {
		String cleaned = name == null ? "" : name.replaceAll("\\s+", " ").replaceAll("[^\\p{L}\\p{N} '()_\\-.!]", "").replaceAll(" +", " ").trim();
		if (cleaned.length() > MAX_NAME_LENGTH) {
			cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();
		}
		if (cleaned.isEmpty()) {
			throw new IllegalArgumentException("Preset name is empty");
		}
		return cleaned;
	}

	static Preset parse(String json) {
		JsonElement element;
		try {
			element = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("Not valid JSON");
		}
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("Not a sky preset");
		}
		JsonObject root = element.getAsJsonObject();
		if (!root.has("format") || !FORMAT.equals(root.get("format").getAsString())) {
			throw new IllegalArgumentException("Not a Holo sky preset (missing \"format\": \"" + FORMAT + "\")");
		}
		if (!root.has("values") || !root.get("values").isJsonObject()) {
			throw new IllegalArgumentException("Preset has no values");
		}
		String name = sanitizeName(root.has("name") ? root.get("name").getAsString() : "Imported Sky");
		String description = root.has("description") ? root.get("description").getAsString() : "";
		return new Preset(name, description, false, root.getAsJsonObject("values"));
	}

	private void write(Preset preset) {
		try {
			Files.createDirectories(directory);
			Path target = directory.resolve(slug(preset.name()) + ".json");
			Path temp = target.resolveSibling(target.getFileName() + ".tmp");
			Files.writeString(temp, export(preset), StandardCharsets.UTF_8);
			try {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.accept("Could not save sky preset " + preset.name() + ": " + e.getMessage());
		}
	}

	private void deleteFile(String name) {
		try {
			Files.deleteIfExists(directory.resolve(slug(name) + ".json"));
		} catch (IOException e) {
			log.accept("Could not delete sky preset file for " + name + ": " + e.getMessage());
		}
	}

	public static String slug(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return slug.isEmpty() ? "preset" : slug;
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
