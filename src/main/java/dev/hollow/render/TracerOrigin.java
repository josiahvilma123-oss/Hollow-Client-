package dev.hollow.render;

import dev.hollow.core.setting.EnumSetting;

public enum TracerOrigin implements EnumSetting.Displayable {
	CROSSHAIR("Crosshair"),
	SCREEN_BOTTOM("Screen Bottom"),
	SCREEN_TOP("Screen Top");

	private final String displayName;

	TracerOrigin(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
