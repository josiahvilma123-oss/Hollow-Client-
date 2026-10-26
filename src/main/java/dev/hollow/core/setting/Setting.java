package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A single configurable value owned by a module. Settings know how to validate,
 * serialize and describe themselves so the GUI and config layers stay generic.
 */
public abstract class Setting<T> {
	private final String id;
	private final String name;
	private final String description;
	private final T defaultValue;
	private final List<Consumer<T>> listeners = new ArrayList<>();
	private T value;
	private String group = "";
	private BooleanSupplier visibility = () -> true;

	protected Setting(String id, String name, String description, T defaultValue) {
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.description = description == null ? "" : description;
		this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
		this.value = defaultValue;
	}

	public final String id() {
		return id;
	}

	public final String name() {
		return name;
	}

	public final String description() {
		return description;
	}

	public final String group() {
		return group;
	}

	/** Places this setting under a section header in the settings page. */
	public Setting<T> group(String group) {
		this.group = group == null ? "" : group;
		return this;
	}

	public Setting<T> visibleWhen(BooleanSupplier visibility) {
		this.visibility = Objects.requireNonNull(visibility);
		return this;
	}

	/** Adds a further visibility condition on top of the existing one. */
	public Setting<T> alsoVisibleWhen(BooleanSupplier condition) {
		BooleanSupplier previous = visibility;
		this.visibility = () -> previous.getAsBoolean() && condition.getAsBoolean();
		return this;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	public T get() {
		return value;
	}

	public T defaultValue() {
		return defaultValue;
	}

	public void set(T newValue) {
		T sanitized = sanitize(Objects.requireNonNull(newValue, "value"));
		if (Objects.equals(sanitized, value)) {
			return;
		}
		value = sanitized;
		for (Consumer<T> listener : List.copyOf(listeners)) {
			listener.accept(sanitized);
		}
	}

	public void reset() {
		set(defaultValue);
	}

	public boolean isDefault() {
		return Objects.equals(value, defaultValue);
	}

	public Setting<T> onChange(Consumer<T> listener) {
		listeners.add(Objects.requireNonNull(listener));
		return this;
	}

	/** Whether this setting is persisted to the config file. */
	public boolean persistent() {
		return true;
	}

	/** Case-insensitive match against name, description and group, used by settings search. */
	public boolean matches(String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String q = query.toLowerCase(Locale.ROOT).trim();
		return name.toLowerCase(Locale.ROOT).contains(q)
			|| description.toLowerCase(Locale.ROOT).contains(q)
			|| group.toLowerCase(Locale.ROOT).contains(q);
	}

	protected T sanitize(T candidate) {
		return candidate;
	}

	public abstract JsonElement toJson();

	/** The default value in config form, computed without touching the current value or listeners. */
	public final JsonElement defaultJson() {
		T saved = value;
		value = defaultValue;
		try {
			return toJson();
		} finally {
			value = saved;
		}
	}

	/** Loads a value from JSON. Invalid input is ignored so a damaged config never crashes the client. */
	public abstract void fromJson(JsonElement json);
}
