package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A keyboard key bound to an action, stored as a GLFW key code. {@link #NONE} means unbound. */
public class KeySetting extends Setting<Integer> {
	public static final int NONE = -1;

	public KeySetting(String id, String name, String description, int defaultKey) {
		super(id, name, description, defaultKey);
	}

	public boolean isBound() {
		return get() != NONE;
	}

	public void unbind() {
		set(NONE);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsInt());
		}
	}
}
