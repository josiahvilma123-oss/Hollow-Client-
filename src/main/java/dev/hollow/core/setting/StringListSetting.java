package dev.hollow.core.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * An ordered, duplicate-free list of strings (block ids, entity ids, server addresses).
 * Entries are trimmed and lower-cased; entries rejected by the validator are dropped.
 */
public class StringListSetting extends Setting<List<String>> {
	private final Predicate<String> validator;
	private final String placeholder;

	public StringListSetting(String id, String name, String description, List<String> defaults, Predicate<String> validator, String placeholder) {
		super(id, name, description, normalize(defaults, validator));
		this.validator = validator;
		this.placeholder = placeholder == null ? "" : placeholder;
	}

	public String placeholder() {
		return placeholder;
	}

	public boolean isValidEntry(String entry) {
		String normalized = normalizeEntry(entry);
		return !normalized.isEmpty() && validator.test(normalized);
	}

	public boolean add(String entry) {
		String normalized = normalizeEntry(entry);
		if (normalized.isEmpty() || !validator.test(normalized) || get().contains(normalized)) {
			return false;
		}
		List<String> copy = new ArrayList<>(get());
		copy.add(normalized);
		set(copy);
		return true;
	}

	public boolean remove(String entry) {
		String normalized = normalizeEntry(entry);
		if (!get().contains(normalized)) {
			return false;
		}
		List<String> copy = new ArrayList<>(get());
		copy.remove(normalized);
		set(copy);
		return true;
	}

	public boolean contains(String entry) {
		return get().contains(normalizeEntry(entry));
	}

	@Override
	protected List<String> sanitize(List<String> candidate) {
		return normalize(candidate, validator);
	}

	private static String normalizeEntry(String entry) {
		return entry == null ? "" : entry.trim().toLowerCase(Locale.ROOT);
	}

	private static List<String> normalize(List<String> entries, Predicate<String> validator) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String entry : entries) {
			String normalized = normalizeEntry(entry);
			if (!normalized.isEmpty() && validator.test(normalized)) {
				unique.add(normalized);
			}
		}
		return List.copyOf(unique);
	}

	@Override
	public JsonElement toJson() {
		JsonArray array = new JsonArray();
		get().forEach(array::add);
		return array;
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json == null || !json.isJsonArray()) {
			return;
		}
		List<String> loaded = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray()) {
			if (element.isJsonPrimitive()) {
				loaded.add(element.getAsString());
			}
		}
		set(loaded);
	}
}
