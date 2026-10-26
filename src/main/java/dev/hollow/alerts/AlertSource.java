package dev.hollow.alerts;

import dev.hollow.core.setting.EnumSetting;

/** Where an alert came from. Each source has its own enable, sound and cooldown settings. */
public enum AlertSource implements EnumSetting.Displayable {
	BLOCKS("blocks", "Blocks"),
	HOSTILE("hostile", "Hostile Mobs"),
	PASSIVE("passive", "Passive Mobs & Animals"),
	NEUTRAL("neutral", "Villagers & Neutral"),
	PLAYERS("players", "Players");

	private final String id;
	private final String displayName;

	AlertSource(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String id() {
		return id;
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
