package dev.hollow.character;

import dev.hollow.core.setting.EnumSetting;

/** Character cosmetic sets. Cosmetic only: drawn on the local player, never sent to servers. */
public enum Cosmetic implements EnumSetting.Displayable {
	NONE("None", "", 0xFF9D4EDD),
	PHARAOH("Pharaoh", "pharaoh", 0xFFE6B54A),
	WRAITH("Wraith", "wraith", 0xFF7B3FD0),
	HOLLOW_KNIGHT("Hollow Knight", "hollow_knight", 0xFFC9A0FF);

	private final String displayName;
	private final String asset;
	private final int accent;

	Cosmetic(String displayName, String asset, int accent) {
		this.displayName = displayName;
		this.asset = asset;
		this.accent = accent;
	}

	@Override
	public String displayName() {
		return displayName;
	}

	/** Base name of this set's textures and artwork. */
	public String asset() {
		return asset;
	}

	public int accent() {
		return accent;
	}
}
