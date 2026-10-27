package dev.hollow.core.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.function.IntFunction;

/**
 * A key or mouse button, optionally with Ctrl, Shift and Alt held. Codes are GLFW key codes
 * for keys and GLFW button numbers (0-7) for the mouse. {@link #NONE} means unbound.
 */
public record Keybind(int code, boolean mouse, int modifiers) {
	public static final int CTRL = 1;
	public static final int SHIFT = 2;
	public static final int ALT = 4;
	public static final Keybind NONE = new Keybind(-1, false, 0);
	private static final int MAX_MOUSE_BUTTON = 7;

	public Keybind {
		if (code < 0) {
			code = -1;
			mouse = false;
			modifiers = 0;
		}
		if (mouse && code > MAX_MOUSE_BUTTON) {
			code = -1;
			mouse = false;
			modifiers = 0;
		}
		modifiers &= CTRL | SHIFT | ALT;
	}

	public static Keybind key(int code) {
		return new Keybind(code, false, 0);
	}

	public static Keybind key(int code, int modifiers) {
		return new Keybind(code, false, modifiers);
	}

	public static Keybind mouse(int button) {
		return new Keybind(button, true, 0);
	}

	public boolean isBound() {
		return code >= 0;
	}

	public boolean hasModifier(int modifier) {
		return (modifiers & modifier) != 0;
	}

	/** Human-readable form such as "Ctrl + Shift + G" or "Mouse 4". */
	public String describe(IntFunction<String> keyName, IntFunction<String> buttonName) {
		if (!isBound()) {
			return "None";
		}
		StringBuilder text = new StringBuilder();
		if (hasModifier(CTRL)) {
			text.append("Ctrl + ");
		}
		if (hasModifier(SHIFT)) {
			text.append("Shift + ");
		}
		if (hasModifier(ALT)) {
			text.append("Alt + ");
		}
		text.append(mouse ? buttonName.apply(code) : keyName.apply(code));
		return text.toString();
	}

	public JsonElement toJson() {
		if (!mouse && modifiers == 0) {
			// Plain keys keep the original numeric form, so older Holo versions can still read them.
			return new JsonPrimitive(code);
		}
		JsonObject object = new JsonObject();
		object.addProperty("code", code);
		object.addProperty("mouse", mouse);
		object.addProperty("mods", modifiers);
		return object;
	}

	/** Reads both the original numeric form and the object form; anything else gives null. */
	public static Keybind fromJson(JsonElement json) {
		if (json == null) {
			return null;
		}
		try {
			if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
				return key(json.getAsInt());
			}
			if (json.isJsonObject()) {
				JsonObject object = json.getAsJsonObject();
				int code = object.get("code").getAsInt();
				boolean mouse = object.has("mouse") && object.get("mouse").getAsBoolean();
				int mods = object.has("mods") ? object.get("mods").getAsInt() : 0;
				return new Keybind(code, mouse, mods);
			}
		} catch (RuntimeException e) {
			return null;
		}
		return null;
	}
}
