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
}
