package dev.hollow.gui;

/** Two-step confirmation for destructive buttons: the first click arms it for a few seconds. */
final class Confirm {
	private static final long WINDOW_MS = 3500;
	private long armedAt = -1;
	private final String label;
	private final String confirmLabel;

	Confirm(String label, String confirmLabel) {
		this.label = label;
		this.confirmLabel = confirmLabel;
	}

	boolean armed() {
		return armedAt >= 0 && System.currentTimeMillis() - armedAt < WINDOW_MS;
	}

	String label() {
		return armed() ? confirmLabel : label;
	}

	/** Returns true when the action should run (second click while armed). */
	boolean click() {
		if (armed()) {
			armedAt = -1;
			return true;
		}
		armedAt = System.currentTimeMillis();
		return false;
	}

	void disarm() {
		armedAt = -1;
	}
}
