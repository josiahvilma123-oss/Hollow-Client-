package dev.hollow.alerts;

import dev.hollow.core.setting.EnumSetting;

public enum AlertSound implements EnumSetting.Displayable {
	CHIME("Amethyst Chime"),
	BELL("Bell"),
	SOUL("Soul Whisper"),
	PLING("Pling"),
	ORB("Orb");

	private final String displayName;

	AlertSound(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
