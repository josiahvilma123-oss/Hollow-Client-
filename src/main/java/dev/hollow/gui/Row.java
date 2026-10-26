package dev.hollow.gui;

import dev.hollow.core.setting.Setting;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** One line (or block) on a settings page. Positions are assigned by the page layout every frame. */
abstract class Row {
	protected static final int PAD = 8;
	int x;
	int y;
	int width;
	double hover;
	/** Context of the last layout pass; available to input handlers. */
	UiContext ui;

	/** The setting this row edits, or null for headers and previews. */
	Setting<?> setting() {
		return null;
	}

	abstract int height(UiContext ui);

	abstract void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY);

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		return false;
	}

	void mouseReleased() {
	}

	boolean keyPressed(KeyEvent event) {
		return false;
	}

	boolean charTyped(CharacterEvent event) {
		return false;
	}

	/** True while the row owns keyboard input (text entry, key capture). */
	boolean capturesKeyboard() {
		return false;
	}

	/** Drops keyboard focus; returns true if something was cancelled. */
	boolean cancelInput() {
		return false;
	}

	boolean contains(double mouseX, double mouseY, UiContext ui) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height(ui);
	}

	/** Base height of a setting row: one line, plus one for the description when shown. */
	protected int baseHeight(UiContext ui) {
		Setting<?> setting = setting();
		boolean hasDescription = ui.descriptions && setting != null && !setting.description().isEmpty();
		return hasDescription ? 28 : 18;
	}

	/** Draws the hover card, label and description shared by setting rows. */
	protected void renderLabel(GuiGraphics g, UiContext ui, int mouseX, int mouseY, int controlWidth) {
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height(ui);
		hover = ui.approach(hover, hovered ? 1 : 0, 0.06);
		int h = height(ui);
		if (hover > 0.01) {
			Draw2D.panel(g, x, y, width, h, ui.c(dev.hollow.core.util.Colors.withAlpha(Theme.CARD_HOVER, (int) (hover * 255))));
		}
		Setting<?> setting = setting();
		int textWidth = width - controlWidth - PAD * 3;
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, setting.name(), textWidth), x + PAD, y + 5, ui.c(Theme.TEXT));
		if (ui.descriptions && !setting.description().isEmpty()) {
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, setting.description(), width - PAD * 2), x + PAD, y + 16, ui.c(Theme.TEXT_FAINT));
		}
	}

	protected boolean over(double mouseX, double mouseY, int rx, int ry, int rw, int rh) {
		return mouseX >= rx && mouseX < rx + rw && mouseY >= ry && mouseY < ry + rh;
	}
}
