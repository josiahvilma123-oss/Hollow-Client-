package dev.hollow.core.module;

/** Sections of the Holo menu's sidebar, in display order. */
public enum Category {
	COMBAT("Combat", "Kill Aura, Trigger Bot, reach display and knockback testing"),
	AIM_ASSIST("Aim Assist", "Smooth, configurable aim assistance"),
	BRIDGING("Bridging", "Auto Bridge and Speed Bridge"),
	MOVEMENT("Movement", "Movement assistance"),
	RENDER("Render", "Motion blur, tracers and the free camera"),
	WORLD("World", "Block highlights and custom skies"),
	EXPLORATION("Exploration", "Waypoints, bases, the world scanner and visibility diagnostics"),
	PLAYER("Player", "Hand animations and character outfits"),
	PERFORMANCE("Performance", "Presets, FPS Boost and adaptive quality"),
	HUD("HUD", "Alerts, the performance monitor and HUD layout"),
	SETTINGS("Settings", "Interface, menus, theme packs, profiles and server rules");

	private final String displayName;
	private final String description;

	Category(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}
}
