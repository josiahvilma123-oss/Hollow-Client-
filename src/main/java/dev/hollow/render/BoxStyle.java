package dev.hollow.render;

import dev.hollow.core.setting.EnumSetting;

public enum BoxStyle implements EnumSetting.Displayable {
	OUTLINE("Outline"),
	FILLED("Filled"),
	BOTH("Outline + Fill");

	private final String displayName;

	BoxStyle(String displayName) {
		this.displayName = displayName;
	}

	public boolean outline() {
		return this != FILLED;
	}

	public boolean fill() {
		return this != OUTLINE;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
