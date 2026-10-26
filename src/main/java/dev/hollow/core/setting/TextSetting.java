package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.Predicate;

/** A short free-text value such as a resource id or file name. Invalid input is rejected. */
public class TextSetting extends Setting<String> {
	private final Predicate<String> validator;
	private final int maxLength;
	private final String placeholder;

	public TextSetting(String id, String name, String description, String defaultValue, int maxLength, Predicate<String> validator,
			String placeholder) {
		super(id, name, description, defaultValue);
		this.validator = validator;
		this.maxLength = maxLength;
		this.placeholder = placeholder == null ? "" : placeholder;
	}

	public int maxLength() {
		return maxLength;
	}

	public String placeholder() {
		return placeholder;
	}

	public boolean isValid(String candidate) {
		String trimmed = candidate == null ? "" : candidate.trim();
		return trimmed.length() <= maxLength && (trimmed.isEmpty() || validator.test(trimmed));
	}

	@Override
	protected String sanitize(String candidate) {
		String trimmed = candidate.trim();
		return isValid(trimmed) ? trimmed : get();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) {
			set(json.getAsString());
		}
	}
}
