package dev.hollow.core.module;

import dev.hollow.core.input.BindMode;
import dev.hollow.core.input.Keybind;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for every Holo feature. A module owns its settings, knows whether it is
 * enabled and must restore vanilla behaviour in {@link #onDisable()}.
 */
public abstract class Module {
	private final String id;
	private final String name;
	private final String description;
	private final Category category;
	private final boolean defaultEnabled;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final KeySetting keybind;
	private final EnumSetting<BindMode> bindMode;
	private final List<Runnable> stateListeners = new ArrayList<>();
	private boolean enabled;
	private String unavailableReason;

	protected Module(String id, String name, String description, Category category, boolean defaultEnabled) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.category = Objects.requireNonNull(category);
		this.defaultEnabled = defaultEnabled;
		this.keybind = new KeySetting("keybind", "Key", "Key, mouse button or combination that controls " + name
			+ ". Ignored while a menu or text field is open.", Keybind.NONE);
		this.keybind.group("Controls");
		this.bindMode = new EnumSetting<>("bind_mode", "Key Mode", "Toggle: press to switch on or off. Hold: on only while held.",
			BindMode.TOGGLE);
		this.bindMode.group("Controls");
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

	public final Category category() {
		return category;
	}

	public final boolean defaultEnabled() {
		return defaultEnabled;
	}

	public final KeySetting keybind() {
		return keybind;
	}

	public final EnumSetting<BindMode> bindMode() {
		return bindMode;
	}

	/**
	 * Why the module can't act right now even though it is enabled (for example a server that
	 * doesn't permit it), or empty. Unlike {@link #unavailableReason()} this can change at any time.
	 */
	public Optional<String> blockedReason() {
		return Optional.empty();
	}

	/** Restores every setting (including the keybind) to its default value. */
	public final void resetSettings() {
		for (Setting<?> setting : settings()) {
			if (setting.persistent()) {
				setting.reset();
			}
		}
	}

	/** Whether the module may be toggled by the user. Modules without a user-facing toggle return false. */
	public boolean isToggleable() {
		return true;
	}

	public final boolean isEnabled() {
		return enabled && unavailableReason == null;
	}

	public final void setEnabled(boolean value) {
		if (value && unavailableReason != null) {
			return;
		}
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (value) {
			onEnable();
		} else {
			onDisable();
		}
		stateListeners.forEach(Runnable::run);
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	public final boolean isAvailable() {
		return unavailableReason == null;
	}

	public final Optional<String> unavailableReason() {
		return Optional.ofNullable(unavailableReason);
	}

	/**
	 * Disables the module permanently for this session, for example when a required render
	 * hook could not be applied because of an incompatible mod.
	 */
	public final void markUnavailable(String reason) {
		boolean wasEnabled = isEnabled();
		unavailableReason = Objects.requireNonNull(reason);
		if (wasEnabled) {
			onDisable();
		}
		stateListeners.forEach(Runnable::run);
	}

	public final void onStateChange(Runnable listener) {
		stateListeners.add(listener);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	protected final <S extends Setting<?>> S add(S setting) {
		for (Setting<?> existing : settings) {
			if (existing.id().equals(setting.id())) {
				throw new IllegalArgumentException("Duplicate setting id '" + setting.id() + "' in module " + id);
			}
		}
		settings.add(setting);
		return setting;
	}

	/** Adds a setting under a section header on the module's settings page. */
	protected final <S extends Setting<?>> S add(String group, S setting) {
		setting.group(group);
		return add(setting);
	}

	/** All settings, plus the keybind and its mode at the end when the module is toggleable. */
	public final List<Setting<?>> settings() {
		if (!isToggleable()) {
			return Collections.unmodifiableList(settings);
		}
		List<Setting<?>> all = new ArrayList<>(settings);
		all.add(keybind);
		all.add(bindMode);
		return Collections.unmodifiableList(all);
	}

	public final Optional<Setting<?>> setting(String settingId) {
		return settings().stream().filter(s -> s.id().equals(settingId)).findFirst();
	}

	public boolean matches(String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String q = query.toLowerCase(Locale.ROOT).trim();
		if (name.toLowerCase(Locale.ROOT).contains(q) || description.toLowerCase(Locale.ROOT).contains(q)) {
			return true;
		}
		return settings().stream().anyMatch(s -> s.matches(q));
	}
}
