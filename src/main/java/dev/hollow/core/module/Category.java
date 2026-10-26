package dev.hollow.core.module;

public enum Category {
	VISUALS_DETECTION("Visuals & Detection", "Motion blur, hand animations, block and entity detection"),
	STUDIO("Studio", "Skies, characters and coordinated theme packs"),
	CLIENT("Client", "Interface and server rule settings");

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
