package dev.hollow.modules;

import dev.hollow.bridge.BridgeAssist;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.policy.Automation;
import java.util.Optional;

/**
 * Timing help for speed bridging (crouch-and-place). Safe Bridge holds sneak at edges for you;
 * Rhythm Bridge only shows and sounds the moment to place; Practice Bridge adds statistics.
 * Optional automatic placement is limited to single-player and permitted servers. Use Hold mode
 * on the keybind to bridge only while a key is held.
 */
public final class SpeedBridgeModule extends Module implements Tickable {
	public enum Preset implements EnumSetting.Displayable {
		SAFE("Safe Bridge"),
		RHYTHM("Rhythm Bridge"),
		PRACTICE("Practice Bridge");

		private final String label;

		Preset(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public final EnumSetting<Preset> preset = add("Mode", new EnumSetting<>("preset", "Preset",
		"Safe: sneak is held at edges. Rhythm: a visual and audio cue marks when to place; you do everything. "
			+ "Practice: like Rhythm, with statistics.", Preset.SAFE));
	public final BoolSetting autoPlace = add("Mode", new BoolSetting("auto_place", "Automatic Placement",
		"Place the block at the edge for you (single-player and permitted servers only).", false));
	public final NumberSetting cueLead = add("Timing", new NumberSetting("cue_lead", "Cue Lead",
		"How far before the edge the cue appears.", 0.15, 0, 0.5, 0.01).unit(" blocks"));
	public final BoolSetting cueSound = add("Timing", new BoolSetting("cue_sound", "Cue Sound", "A soft click when it is time to place.", true));
	public final BoolSetting edgeIndicator = add("Display", new BoolSetting("edge_indicator", "Edge Indicator",
		"Highlight the edge of the block you are standing on.", true));
	public final BoolSetting preview = add("Display", new BoolSetting("preview", "Placement Preview",
		"Outline where the next block goes.", true));
	public final BoolSetting showHud = add("Display", new BoolSetting("show_hud", "Bridge HUD", "Cue, blocks left and practice statistics.", true));
	public final ActionSetting resetStats = add("Display", new ActionSetting("reset_stats", "Practice Statistics",
		"Blocks placed, speed, longest bridge and falls.", "Reset", BridgeAssist.INSTANCE::resetStats));

	public SpeedBridgeModule() {
		super("speed_bridge", "Speed Bridge", "Crouch-and-place timing help with Safe, Rhythm and Practice presets.",
			Category.BRIDGING, false);
	}

	/** Whether this module wants sneak held at edges right now. */
	public boolean sneakAssistActive() {
		return preset.get() == Preset.SAFE || autoPlace.get();
	}

	/** Automation is only needed for Safe sneaking or automatic placement; cues work everywhere. */
	public boolean needsAutomation() {
		return sneakAssistActive();
	}

	@Override
	public Optional<String> blockedReason() {
		return needsAutomation() ? Automation.blockedReason() : Optional.empty();
	}

	@Override
	public void tick() {
		if (isEnabled()) {
			BridgeAssist.INSTANCE.tickSpeed(this);
		}
	}
}
