package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.hollow.core.util.Colors;

/** An ARGB color. Stored in config as a "#AARRGGBB" hex string. */
public class ColorSetting extends Setting<Integer> {
	private final boolean alpha;

	public ColorSetting(String id, String name, String description, int defaultArgb, boolean alpha) {
		super(id, name, description, alpha ? defaultArgb : defaultArgb | 0xFF000000);
		this.alpha = alpha;
	}

	public ColorSetting(String id, String name, String description, int defaultArgb) {
		this(id, name, description, defaultArgb, true);
	}

	public boolean allowsAlpha() {
		return alpha;
	}

	public int argb() {
		return get();
	}

	@Override
	protected Integer sanitize(Integer candidate) {
		return alpha ? candidate : candidate | 0xFF000000;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(Colors.toHex(get()));
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json == null || !json.isJsonPrimitive()) {
			return;
		}
		if (json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsInt());
			return;
		}
		Integer parsed = Colors.parseHex(json.getAsString());
		if (parsed != null) {
			set(parsed);
		}
	}
}
