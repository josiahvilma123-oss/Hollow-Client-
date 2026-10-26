package dev.hollow.modules;

import dev.hollow.core.anim.Easing;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.hands.EquipStyle;
import dev.hollow.hands.HandAnimator;
import dev.hollow.hands.HandPreset;
import java.util.List;

public final class HollowHandsModule extends Module {
	public final EnumSetting<HandPreset> preset = add("Preset", new EnumSetting<>("preset", "Preset",
		"Subtle, Smooth and Stylized tune every slider below at once. Editing a slider switches to Custom.", HandPreset.SMOOTH));

	public final NumberSetting swingSpeed = add("Swing", new NumberSetting("swing_speed", "Swing Speed",
		"Visual speed of swing and attack animations. Attack timing itself is unchanged.", 0.9, 0.4, 2.0, 0.05).unit("x"));
	public final NumberSetting swingIntensity = add("Swing", new NumberSetting("swing_intensity", "Swing Intensity",
		"How wide the swing arc is.", 1.0, 0.5, 1.5, 0.05).unit("x"));
	public final EnumSetting<Easing> swingEasing = add("Swing", new EnumSetting<>("swing_easing", "Swing Curve",
		"Easing curve applied to swings.", Easing.CUBIC_IN_OUT));

	public final NumberSetting bob = add("Movement", new NumberSetting("bob", "Walk Bob",
		"Hand bobbing while walking, in step with view bobbing.", 0.6, 0, 1.5, 0.05));
	public final NumberSetting sprint = add("Movement", new NumberSetting("sprint", "Sprint Lean",
		"How much the item lowers and tilts while sprinting.", 0.7, 0, 1.5, 0.05));
	public final NumberSetting idleSway = add("Movement", new NumberSetting("idle_sway", "Idle Sway",
		"Faint breathing motion while standing still.", 0.6, 0, 1.5, 0.05));
	public final NumberSetting stateSmoothing = add("Movement", new NumberSetting("state_smoothing", "State Blend",
		"Time to blend between idle, walking and sprinting.", 0.15, 0.05, 0.5, 0.01).unit("s"));

	public final EnumSetting<EquipStyle> equipStyle = add("Transitions", new EnumSetting<>("equip_style", "Equip Style",
		"Animation when switching items.", EquipStyle.SMOOTH));
	public final NumberSetting useTransition = add("Transitions", new NumberSetting("use_transition", "Use Transition",
		"Softens the snap into and out of eating, blocking and drawing a bow.", 160, 0, 400, 10).unit("ms"));

	public final NumberSetting offsetX = add("Position", new NumberSetting("offset_x", "Offset X", "Move held items left or right.", 0, -0.5, 0.5, 0.01));
	public final NumberSetting offsetY = add("Position", new NumberSetting("offset_y", "Offset Y", "Move held items up or down.", 0, -0.5, 0.5, 0.01));
	public final NumberSetting offsetZ = add("Position", new NumberSetting("offset_z", "Offset Z", "Move held items closer or further.", 0, -0.5, 0.5, 0.01));
	public final NumberSetting scale = add("Position", new NumberSetting("scale", "Scale", "Size of held items.", 1.0, 0.5, 1.5, 0.01).unit("x"));
	public final NumberSetting rotX = add("Position", new NumberSetting("rot_x", "Pitch", "Tilt held items forward or back.", 0, -45, 45, 1).unit("°"));
	public final NumberSetting rotY = add("Position", new NumberSetting("rot_y", "Yaw", "Turn held items inward or outward.", 0, -45, 45, 1).unit("°"));
	public final NumberSetting rotZ = add("Position", new NumberSetting("rot_z", "Roll", "Roll held items.", 0, -45, 45, 1).unit("°"));
	public final ActionSetting resetPosition = add("Position", new ActionSetting("reset_position", "Reset Position",
		"Restore default offset, scale and rotation.", "Reset", this::resetPosition));

	public final BoolSetting trails = add("Trails", new BoolSetting("trails", "Swing Trails",
		"A faint purple ribbon following the item tip during swings.", false));
	public final ColorSetting trailColor = add("Trails", new ColorSetting("trail_color", "Trail Color", "", 0x99B57BFF));
	public final NumberSetting trailLength = add("Trails", new NumberSetting("trail_length", "Trail Length",
		"How long trail segments stay visible.", 140, 60, 400, 10).unit("ms"));

	private final HandAnimator animator = new HandAnimator();
	private boolean applyingPreset;

	public HollowHandsModule() {
		super("hollow_hands", "Hollow Hands", "Smooth swings, bobbing, equip transitions and hand positioning.",
			Category.VISUALS_DETECTION, true);
		preset.onChange(this::applyPreset);
		for (Setting<?> setting : presetControlled()) {
			setting.onChange(value -> {
				if (!applyingPreset) {
					preset.set(HandPreset.CUSTOM);
				}
			});
		}
	}

	private List<Setting<?>> presetControlled() {
		return List.of(swingSpeed, swingIntensity, swingEasing, bob, sprint, idleSway, equipStyle, useTransition, trails);
	}

	private void applyPreset(HandPreset value) {
		if (value == HandPreset.CUSTOM) {
			return;
		}
		applyingPreset = true;
		try {
			HandPresets.apply(value, this);
		} finally {
			applyingPreset = false;
		}
	}

	private void resetPosition() {
		offsetX.reset();
		offsetY.reset();
		offsetZ.reset();
		scale.reset();
		rotX.reset();
		rotY.reset();
		rotZ.reset();
	}

	/** The animator, refreshed with the current settings. */
	public HandAnimator animator() {
		HandAnimator.Config c = animator.config();
		c.swingSpeed = swingSpeed.get();
		c.swingIntensity = swingIntensity.get();
		c.swingEasing = swingEasing.get();
		c.bob = bob.get();
		c.sprint = sprint.get();
		c.idleSway = idleSway.get();
		c.stateSmoothing = stateSmoothing.get();
		c.equipStyle = equipStyle.get();
		c.useTransitionMillis = useTransition.get();
		c.offsetX = offsetX.get();
		c.offsetY = offsetY.get();
		c.offsetZ = offsetZ.get();
		c.scale = scale.get();
		c.rotX = rotX.get();
		c.rotY = rotY.get();
		c.rotZ = rotZ.get();
		return animator;
	}
}
