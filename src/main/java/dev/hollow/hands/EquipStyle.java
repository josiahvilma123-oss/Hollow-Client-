package dev.hollow.hands;

import dev.hollow.core.setting.EnumSetting;

public enum EquipStyle implements EnumSetting.Displayable {
	VANILLA("Vanilla"),
	SMOOTH("Smooth Dip"),
	SLIDE("Side Slide"),
	NONE("Instant");

	private final String displayName;

	EquipStyle(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
