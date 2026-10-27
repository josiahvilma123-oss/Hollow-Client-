package dev.hollow.core.waypoints;

import com.google.gson.JsonObject;
import dev.hollow.core.setting.EnumSetting;
import java.util.Locale;
import java.util.Objects;

/** A saved location: a base, point of interest, scouted spot or death point. */
public record Waypoint(String id, String name, int x, int y, int z, String dimension, Category category, int color, String note, long created,
		boolean visible) {
	public static final int MAX_NAME = 32;
	public static final int MAX_NOTE = 200;
	public static final int MAX_HORIZONTAL = 30_000_000;
	public static final int MAX_VERTICAL = 4096;

	public enum Category implements EnumSetting.Displayable {
		BASE("Base", 0xFFE6B54A),
		POI("Point of Interest", 0xFF9D4EDD),
		SCOUTED("Scouted", 0xFF5FC8FF),
		DEATH("Death", 0xFFE0405E),
		OTHER("Other", 0xFFB9B6C8);

		private final String label;
		private final int color;

		Category(String label, int color) {
			this.label = label;
			this.color = color;
		}

		@Override
		public String displayName() {
			return label;
		}

		public int color() {
			return color;
		}
	}

	public Waypoint {
		Objects.requireNonNull(id);
		name = clean(name, MAX_NAME);
		if (name.isEmpty()) {
			name = "Waypoint";
		}
		note = clean(note, MAX_NOTE);
		x = clamp(x, MAX_HORIZONTAL);
		z = clamp(z, MAX_HORIZONTAL);
		y = clamp(y, MAX_VERTICAL);
		dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.trim().toLowerCase(Locale.ROOT);
		category = category == null ? Category.OTHER : category;
		color = color == 0 ? category.color() : color | 0xFF000000;
	}

	private static String clean(String text, int max) {
		if (text == null) {
			return "";
		}
		String trimmed = text.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
		return trimmed.length() > max ? trimmed.substring(0, max).trim() : trimmed;
	}

	private static int clamp(int value, int limit) {
		return Math.max(-limit, Math.min(limit, value));
	}

	public Waypoint withVisible(boolean value) {
		return new Waypoint(id, name, x, y, z, dimension, category, color, note, created, value);
	}

	public double distanceTo(double px, double py, double pz) {
		double dx = x + 0.5 - px;
		double dy = y + 0.5 - py;
		double dz = z + 0.5 - pz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("id", id);
		object.addProperty("name", name);
		object.addProperty("x", x);
		object.addProperty("y", y);
		object.addProperty("z", z);
		object.addProperty("dimension", dimension);
		object.addProperty("category", category.name());
		object.addProperty("color", String.format(Locale.ROOT, "#%08X", color));
		object.addProperty("note", note);
		object.addProperty("created", created);
		object.addProperty("visible", visible);
		return object;
	}

	/** Reads one waypoint; throws IllegalArgumentException for anything malformed. */
	public static Waypoint fromJson(JsonObject object) {
		try {
			Category category = Category.OTHER;
			if (object.has("category")) {
				try {
					category = Category.valueOf(object.get("category").getAsString().toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					category = Category.OTHER;
				}
			}
			int color = 0;
			if (object.has("color")) {
				String hex = object.get("color").getAsString().replace("#", "");
				color = (int) Long.parseLong(hex, 16);
			}
			return new Waypoint(object.get("id").getAsString(), object.get("name").getAsString(), object.get("x").getAsInt(),
				object.get("y").getAsInt(), object.get("z").getAsInt(), object.has("dimension") ? object.get("dimension").getAsString() : null,
				category, color, object.has("note") ? object.get("note").getAsString() : "",
				object.has("created") ? object.get("created").getAsLong() : 0L, !object.has("visible") || object.get("visible").getAsBoolean());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Malformed waypoint: " + e.getMessage(), e);
		}
	}
}
