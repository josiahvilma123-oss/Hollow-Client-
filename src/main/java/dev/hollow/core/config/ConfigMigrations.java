package dev.hollow.core.config;

import com.google.gson.JsonObject;
import java.util.function.Consumer;

/**
 * Step-by-step upgrades of older config files. Each step only adds or renames data; user
 * values are never discarded.
 */
public final class ConfigMigrations {
	private ConfigMigrations() {
	}

	public static JsonObject migrate(JsonObject root, int fromVersion, Consumer<String> log) {
		JsonObject current = root.deepCopy();
		int version = fromVersion;
		if (version < 2) {
			current = toV2(current);
			version = 2;
		}
		if (version < 3) {
			current = toV3(current);
			version = 3;
		}
		current.addProperty("version", version);
		return current;
	}

	/**
	 * v1 to v2: the Holo update only adds modules, so existing entries carry over unchanged.
	 */
	private static JsonObject toV2(JsonObject root) {
		if (!root.has("modules") || !root.get("modules").isJsonObject()) {
			root.add("modules", new JsonObject());
		}
		return root;
	}

	/**
	 * v2 to v3: the main menu's "Featured Character" used to name a character directly
	 * (PHARAOH, WRAITH, HOLLOW_KNIGHT). It is now "CHOOSE" plus a separate character setting.
	 */
	private static JsonObject toV3(JsonObject root) {
		JsonObject settings = settingsOf(root, "main_menu");
		if (settings != null && settings.has("character") && settings.get("character").isJsonPrimitive()) {
			String old = settings.get("character").getAsString();
			if (old.equals("PHARAOH") || old.equals("WRAITH") || old.equals("HOLLOW_KNIGHT")) {
				settings.addProperty("character", "CHOOSE");
				settings.addProperty("featured_character", old);
			}
		}
		return root;
	}

	private static JsonObject settingsOf(JsonObject root, String moduleId) {
		if (!root.has("modules") || !root.get("modules").isJsonObject()) {
			return null;
		}
		JsonObject modules = root.getAsJsonObject("modules");
		if (!modules.has(moduleId) || !modules.get(moduleId).isJsonObject()) {
			return null;
		}
		JsonObject module = modules.getAsJsonObject(moduleId);
		if (!module.has("settings") || !module.get("settings").isJsonObject()) {
			return null;
		}
		return module.getAsJsonObject("settings");
	}
}
