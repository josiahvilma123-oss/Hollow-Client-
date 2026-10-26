package dev.hollow.hands;

import dev.hollow.core.anim.Easing;
import dev.hollow.core.setting.EnumSetting;

/** Bundled values for the Hollow Hands sliders. CUSTOM means the user tuned them by hand. */
public enum HandPreset implements EnumSetting.Displayable {
	SUBTLE("Subtle", 1.0, 0.9, Easing.SINE, 0.35, 0.4, 0.4, EquipStyle.SMOOTH, 120, false),
	SMOOTH("Smooth", 0.9, 1.0, Easing.CUBIC_IN_OUT, 0.6, 0.7, 0.6, EquipStyle.SMOOTH, 160, false),
	STYLIZED("Stylized", 0.8, 1.2, Easing.BACK_OUT, 0.9, 1.0, 0.8, EquipStyle.SLIDE, 200, true),
	CUSTOM("Custom", 1.0, 1.0, Easing.SINE, 0.5, 0.5, 0.5, EquipStyle.SMOOTH, 150, false);

	private final String displayName;
	private final double swingSpeed;
	private final double swingIntensity;
	private final Easing swingEasing;
	private final double bob;
	private final double sprint;
	private final double idleSway;
	private final EquipStyle equipStyle;
	private final double useTransitionMillis;
	private final boolean trails;

	HandPreset(String displayName, double swingSpeed, double swingIntensity, Easing swingEasing, double bob, double sprint,
			double idleSway, EquipStyle equipStyle, double useTransitionMillis, boolean trails) {
		this.displayName = displayName;
		this.swingSpeed = swingSpeed;
		this.swingIntensity = swingIntensity;
		this.swingEasing = swingEasing;
		this.bob = bob;
		this.sprint = sprint;
		this.idleSway = idleSway;
		this.equipStyle = equipStyle;
		this.useTransitionMillis = useTransitionMillis;
		this.trails = trails;
	}

	public double swingSpeed() {
		return swingSpeed;
	}

	public double swingIntensity() {
		return swingIntensity;
	}

	public Easing swingEasing() {
		return swingEasing;
	}

	public double bob() {
		return bob;
	}

	public double sprint() {
		return sprint;
	}

	public double idleSway() {
		return idleSway;
	}

	public EquipStyle equipStyle() {
		return equipStyle;
	}

	public double useTransitionMillis() {
		return useTransitionMillis;
	}

	public boolean trails() {
		return trails;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
