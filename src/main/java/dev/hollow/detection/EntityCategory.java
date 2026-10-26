package dev.hollow.detection;

import dev.hollow.alerts.AlertSource;
import dev.hollow.core.setting.EnumSetting;

public enum EntityCategory implements EnumSetting.Displayable {
	HOSTILE("hostile", "Hostile Mobs", "Hostile Mob", 0xFFE0405E, AlertSource.HOSTILE),
	PASSIVE("passive", "Passive Mobs & Animals", "Passive Mob", 0xFF5FD68A, AlertSource.PASSIVE),
	NEUTRAL("neutral", "Villagers & Neutral", "Neutral Mob", 0xFFE8C26A, AlertSource.NEUTRAL),
	PLAYERS("players", "Players", "Player", 0xFFB57BFF, AlertSource.PLAYERS),
	CUSTOM("custom", "Custom Filter", "Entity", 0xFF7FD3FF, AlertSource.NEUTRAL);

	private final String id;
	private final String displayName;
	private final String singular;
	private final int defaultColor;
	private final AlertSource alertSource;

	EntityCategory(String id, String displayName, String singular, int defaultColor, AlertSource alertSource) {
		this.id = id;
		this.displayName = displayName;
		this.singular = singular;
		this.defaultColor = defaultColor;
		this.alertSource = alertSource;
	}

	public String id() {
		return id;
	}

	@Override
	public String displayName() {
		return displayName;
	}

	public String singular() {
		return singular;
	}

	public int defaultColor() {
		return defaultColor;
	}

	public AlertSource alertSource() {
		return alertSource;
	}
}
