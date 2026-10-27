package dev.hollow.modules;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.alerts.AlertSound;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import java.util.EnumMap;
import java.util.Map;

public final class HollowAlertsModule extends Module {
	public final EnumSetting<Placement> placement = add("Appearance", new EnumSetting<>("placement", "Position", "Where toasts appear.", Placement.TOP_RIGHT));
	public final NumberSetting lifetime = add("Appearance", new NumberSetting("lifetime", "Display Time", "", 4, 1.5, 10, 0.5).unit("s"));
	public final NumberSetting maxVisible = add("Appearance", new NumberSetting("max_visible", "Max Visible", "Toasts shown at once.", 4, 1, 6, 1));
	public final NumberSetting scale = add("Appearance", new NumberSetting("scale", "Scale", "", 1.0, 0.75, 1.5, 0.05).unit("x"));
	public final BoolSetting chat = add("Appearance", new BoolSetting("chat", "Echo To Chat", "Also print alerts in chat (only visible to you).", false));
	public final ActionSetting preview = add("Appearance", new ActionSetting("preview", "Preview",
		"Show an example alert.", "Preview alert", this::preview));

	public final BoolSetting sound = add("Sound", new BoolSetting("sound", "Sounds", "Master switch for alert sounds.", true));
	public final EnumSetting<AlertSound> soundType = add("Sound", new EnumSetting<>("sound_type", "Sound", "", AlertSound.CHIME));
	public final NumberSetting volume = add("Sound", new NumberSetting("volume", "Volume", "", 0.6, 0, 1, 0.05));
	public final NumberSetting pitch = add("Sound", new NumberSetting("pitch", "Pitch", "", 1.0, 0.5, 2, 0.05));

	private final Map<AlertSource, SourceSettings> sources = new EnumMap<>(AlertSource.class);
	private int previewIndex;

	public HollowAlertsModule() {
		super("hollow_alerts", "Hollow Alerts", "Unified, unobtrusive notifications for block and entity detection.",
			Category.HUD, true);
		for (AlertSource source : AlertSource.values()) {
			sources.put(source, new SourceSettings(source));
		}
		BooleanVisibility.when(sound, soundType, volume, pitch);
	}

	public SourceSettings source(AlertSource source) {
		return sources.get(source);
	}

	private void preview() {
		Alert[] samples = {
			new Alert(AlertSource.BLOCKS, "Amethyst Cluster", "Detected", 12.4, "preview:block"),
			new Alert(AlertSource.HOSTILE, "Hostile Mob", "Nearby", 18.2, "preview:hostile"),
			new Alert(AlertSource.BLOCKS, "Ancient Debris", "Detected", 31.7, "preview:debris"),
		};
		Alert alert = samples[previewIndex++ % samples.length];
		if (Hollow.alerts() != null) {
			Hollow.alerts().preview(alert);
		}
	}

	public final class SourceSettings {
		public final AlertSource source;
		public final BoolSetting enabled;
		public final BoolSetting playSound;
		public final NumberSetting cooldown;

		private SourceSettings(AlertSource source) {
			this.source = source;
			String group = "Source · " + source.displayName();
			this.enabled = add(group, new BoolSetting(source.id() + ".enabled", "Alerts", "Show alerts from " + source.displayName() + ".",
				source != AlertSource.PASSIVE));
			this.playSound = add(group, new BoolSetting(source.id() + ".sound", "Sound", "", source != AlertSource.PASSIVE));
			this.cooldown = add(group, new NumberSetting(source.id() + ".cooldown", "Cooldown",
				"Repeat alerts with the same name are merged within this window.", source == AlertSource.BLOCKS ? 8 : 15, 0, 120, 1).unit("s"));
			BooleanVisibility.when(enabled, playSound, cooldown);
		}
	}

	public enum Placement implements EnumSetting.Displayable {
		TOP_RIGHT("Top Right"),
		TOP_CENTER("Top Center"),
		TOP_LEFT("Top Left"),
		BOTTOM_RIGHT("Bottom Right"),
		CUSTOM("Custom (HUD Editor)");

		private final String displayName;

		Placement(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String displayName() {
			return displayName;
		}
	}
}
