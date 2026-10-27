package dev.hollow.hud;

import net.minecraft.client.gui.GuiGraphics;

/** A movable piece of Holo's HUD. Sizes are in GUI pixels before the HUD scale is applied. */
public abstract class HudElement {
	private final String id;
	private final String name;
	private final float defaultX;
	private final float defaultY;

	protected HudElement(String id, String name, float defaultX, float defaultY) {
		this.id = id;
		this.name = name;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
	}

	public final String id() {
		return id;
	}

	public final String name() {
		return name;
	}

	/** Default position as fractions of the free screen space (0 = left/top, 1 = right/bottom). */
	public final float defaultX() {
		return defaultX;
	}

	public final float defaultY() {
		return defaultY;
	}

	/** Scale the element is drawn at; most follow the global HUD scale. */
	public float scale() {
		return HudElements.scale();
	}

	/**
	 * Elements drawn by another system (such as alert toasts) only take their position from the
	 * layout; the HUD renderer skips them and the editor always shows them.
	 */
	public boolean drawnElsewhere() {
		return false;
	}

	/** Called when the editor moves an element that is drawn elsewhere, e.g. to switch it to a custom position. */
	public void onMoved() {
	}

	public abstract int width();

	public abstract int height();

	/** Whether the element shows in game right now (usually: its module is on). */
	public abstract boolean visible();

	/** Why the element is hidden, shown in the HUD editor (e.g. "Aim Assist is off"). */
	public abstract String hiddenReason();

	/**
	 * Draws the element with its top-left corner at (0, 0). With {@code preview} it shows sample
	 * content for the HUD editor even when there is nothing live to show.
	 */
	public abstract void render(GuiGraphics g, boolean preview);
}
