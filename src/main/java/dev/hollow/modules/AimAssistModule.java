package dev.hollow.modules;

import dev.hollow.combat.AimAssist;
import dev.hollow.combat.TargetSettings;
import dev.hollow.core.input.Keybind;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.policy.Automation;
import java.util.List;
import java.util.Optional;

/**
 * Gently pulls the crosshair towards an eligible target while you aim. It only adds to your own
 * mouse movement (by default it does nothing unless you are moving the mouse), never snaps, and
 * stops in menus, text fields, when the window loses focus, and where automation isn't permitted.
 */
public final class AimAssistModule extends Module implements Tickable {
	public enum Preset implements EnumSetting.Displayable {
		SUBTLE("Subtle", 0.25, 0.35, 60, 30),
		SMOOTH("Smooth", 0.45, 0.22, 120, 45),
		STANDARD("Standard", 0.65, 0.15, 180, 60),
		CUSTOM("Custom", 0, 0, 0, 0);

		private final String label;
		final double strength;
		final double smoothing;
		final double maxSpeed;
		final double fov;

		Preset(String label, double strength, double smoothing, double maxSpeed, double fov) {
			this.label = label;
			this.strength = strength;
			this.smoothing = smoothing;
			this.maxSpeed = maxSpeed;
			this.fov = fov;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum Activation implements EnumSetting.Displayable {
		ALWAYS("While Enabled"),
		WHILE_ATTACKING("While Holding Attack"),
		HOLD_KEY("Hold Activation Key"),
		TOGGLE_KEY("Toggle With Activation Key");

		private final String label;

		Activation(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum AimPoint implements EnumSetting.Displayable {
		CLOSEST("Closest Point"),
		BODY("Body"),
		HEAD("Head");

		private final String label;

		AimPoint(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	private boolean applyingPreset;

	public final EnumSetting<Preset> preset = add("Behaviour", new EnumSetting<>("preset", "Preset",
		"Subtle, Smooth and Standard set strength, smoothing, speed and field of view together. Editing any of them switches to Custom.",
		Preset.SMOOTH));
	public final EnumSetting<Activation> activation = add("Behaviour", new EnumSetting<>("activation", "Activation",
		"When the assist may act. It never acts in menus or text fields.", Activation.ALWAYS));
	public final KeySetting activationKey = add("Behaviour", new KeySetting("activation_key", "Activation Key",
		"Used by the Hold and Toggle activation modes.", Keybind.NONE));
	public final BoolSetting requireMouseMovement = add("Behaviour", new BoolSetting("require_mouse", "Only While Moving the Mouse",
		"The assist only adds to your own aiming; if you stop moving the mouse, it stops too. Recommended.", true));
	public final BoolSetting pauseWhileUsing = add("Behaviour", new BoolSetting("pause_using", "Pause While Using Items",
		"No assistance while eating, blocking, drawing a bow and similar.", true));

	public final NumberSetting strength = add("Strength", new NumberSetting("strength", "Strength",
		"How much of the remaining angle the assist may close. 0.2 is a light nudge, 1.0 the strongest.", 0.45, 0.05, 1.0, 0.05));
	public final NumberSetting smoothing = add("Strength", new NumberSetting("smoothing", "Smoothness",
		"Time constant of the pull. Higher values move the camera more gently.", 0.22, 0.05, 1.0, 0.01).unit("s"));
	public final NumberSetting maxSpeed = add("Strength", new NumberSetting("max_speed", "Speed Limit",
		"The assist never turns the camera faster than this, so it can't snap.", 120, 10, 360, 5).unit("°/s"));
	public final BoolSetting sensitivityAware = add("Strength", new BoolSetting("sensitivity_aware", "Match Mouse Sensitivity",
		"Scale the speed limit with your Minecraft mouse sensitivity, so the assist feels the same at any setting.", true));
	public final BoolSetting horizontal = add("Strength", new BoolSetting("horizontal", "Horizontal Assist", "Left-right correction.", true));
	public final NumberSetting horizontalStrength = add("Strength", new NumberSetting("horizontal_strength", "Horizontal Multiplier",
		"", 1.0, 0.1, 2.0, 0.05).unit("x"));
	public final BoolSetting vertical = add("Strength", new BoolSetting("vertical", "Vertical Assist", "Up-down correction.", true));
	public final NumberSetting verticalStrength = add("Strength", new NumberSetting("vertical_strength", "Vertical Multiplier",
		"Usually lower than horizontal: vertical help feels stronger.", 0.6, 0.1, 2.0, 0.05).unit("x"));

	public final NumberSetting fov = add("Targets", new NumberSetting("fov", "Field of View",
		"Only targets within this cone around your crosshair are considered.", 45, 5, 180, 1).unit("°"));
	public final NumberSetting range = add("Targets", new NumberSetting("range", "Range", "Maximum distance to a target's hitbox.",
		6.0, 1.0, 8.0, 0.25).unit(" blocks"));
	public final EnumSetting<AimPoint> aimPoint = add("Targets", new EnumSetting<>("aim_point", "Aim Point",
		"Closest Point needs the least camera movement and feels most natural.", AimPoint.CLOSEST));
	public final TargetSettings targets = new TargetSettings(true, TargetSettings.Priority.CROSSHAIR);

	public final BoolSetting showMarker = add("Display", new BoolSetting("show_marker", "Target Marker",
		"Brackets around the current target.", true));
	public final BoolSetting showIndicator = add("Display", new BoolSetting("show_indicator", "Status Indicator",
		"HUD element showing whether the assist is active (move it in the HUD editor).", true));

	public final BoolSetting practiceMode = add("Practice (Single-player)", new BoolSetting("practice_mode", "Practice Mode",
		"Only practice targets are tracked, and your hit accuracy is counted.", false));
	public final ActionBarSetting practiceActions = add("Practice (Single-player)", new ActionBarSetting("practice_actions", "Practice Targets",
		"Spawns static and wandering targets around you in your own single-player world.",
		new ActionBarSetting.Button("Spawn targets", AimAssist.INSTANCE::spawnPracticeTargets),
		new ActionBarSetting.Button("Clear targets", AimAssist.INSTANCE::clearPracticeTargets),
		new ActionBarSetting.Button("Reset stats", AimAssist.INSTANCE::resetStats)));

	public AimAssistModule() {
		super("aim_assist", "Aim Assist", "Smooth, configurable aim assistance that adds to your own aiming. "
			+ "Single-player and permitted servers only.", Category.AIM_ASSIST, false);
		for (Setting<?> setting : targets.all()) {
			add("Targets", setting);
		}
		activationKey.visibleWhen(() -> activation.get() == Activation.HOLD_KEY || activation.get() == Activation.TOGGLE_KEY);
		horizontalStrength.visibleWhen(horizontal::get);
		verticalStrength.visibleWhen(vertical::get);
		preset.onChange(this::applyPreset);
		for (NumberSetting tuned : List.of(strength, smoothing, maxSpeed, fov)) {
			tuned.onChange(value -> {
				if (!applyingPreset && preset.get() != Preset.CUSTOM) {
					preset.set(Preset.CUSTOM);
				}
			});
		}
	}

	private void applyPreset(Preset value) {
		if (value == Preset.CUSTOM) {
			return;
		}
		applyingPreset = true;
		try {
			strength.set(value.strength);
			smoothing.set(value.smoothing);
			maxSpeed.set(value.maxSpeed);
			fov.set(value.fov);
		} finally {
			applyingPreset = false;
		}
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.blockedReason();
	}

	@Override
	public void tick() {
		AimAssist.INSTANCE.tick(this);
	}

	@Override
	protected void onDisable() {
		AimAssist.INSTANCE.reset();
	}
}
