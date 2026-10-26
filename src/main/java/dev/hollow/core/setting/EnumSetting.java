package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final List<E> values;

	public EnumSetting(String id, String name, String description, E defaultValue) {
		super(id, name, description, defaultValue);
		this.values = List.of(defaultValue.getDeclaringClass().getEnumConstants());
	}

	public List<E> values() {
		return values;
	}

	public void cycle(int direction) {
		int index = values.indexOf(get());
		int next = Math.floorMod(index + (direction >= 0 ? 1 : -1), values.size());
		set(values.get(next));
	}

	public static String displayName(Enum<?> value) {
		if (value instanceof Displayable displayable) {
			return displayable.displayName();
		}
		String raw = value.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
	}

	public String displayValue() {
		return displayName(get());
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get().name());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json == null || !json.isJsonPrimitive()) {
			return;
		}
		String name = json.getAsString();
		for (E value : values) {
			if (value.name().equalsIgnoreCase(name)) {
				set(value);
				return;
			}
		}
	}

	/** Enums may implement this to provide a friendlier label in the GUI. */
	public interface Displayable {
		String displayName();
	}
}
