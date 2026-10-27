package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import dev.hollow.core.input.Keybind;

/** A key or mouse button (with optional modifiers) bound to an action. */
public class KeySetting extends Setting<Keybind> {
	public KeySetting(String id, String name, String description, Keybind defaultKey) {
		super(id, name, description, defaultKey);
	}

	public boolean isBound() {
		return get().isBound();
	}

	public void unbind() {
		set(Keybind.NONE);
	}

	@Override
	public JsonElement toJson() {
		return get().toJson();
	}

	@Override
	public void fromJson(JsonElement json) {
		Keybind keybind = Keybind.fromJson(json);
		if (keybind != null) {
			set(keybind);
		}
	}
}
