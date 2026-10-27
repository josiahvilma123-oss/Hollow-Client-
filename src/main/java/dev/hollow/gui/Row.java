package dev.hollow.gui;

import dev.hollow.core.setting.Setting;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** One line (or block) on a settings page. Positions are assigned by the page layout every frame. */
abstract class Row {
	protected static final int PAD = 8;
	int x;
	int y;
	int width;
	double hover;
	/** Set by the page while keyboard navigation rests on this row. */
	boolean keyboardFocus;
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

	/** Whether the row is shown at all (hidden settings stay out of the page). */
	boolean visible() {
		Setting<?> setting = setting();
		return setting == null || setting.isVisible();
	}

	/** Whether the row matches a lower-case search query. */
	boolean matches(String query) {
		Setting<?> setting = setting();
		return setting == null || setting.matches(query);
	}

	/** Whether keyboard navigation can rest on this row. */
	boolean focusable() {
		return true;
	}

	/** Enter / Space while focused. Returns true if something happened. */
	boolean activate() {
		return false;
	}

	/** Left / Right while focused. */
	boolean adjust(int direction) {
		return false;
	}

	boolean contains(double mouseX, double mouseY, UiContext ui) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height(ui);
	}

	private static final int MAX_DESCRIPTION_LINES = 3;
	private List<FormattedCharSequence> wrapped = List.of();
	private int wrappedWidth = -1;

	/** Description wrapped to the row width, cached until the width changes. */
	protected List<FormattedCharSequence> descriptionLines(UiContext ui) {
		Setting<?> setting = setting();
		if (!ui.descriptions || setting == null || setting.description().isEmpty()) {
			return List.of();
		}
		int available = width - PAD * 2;
		if (available != wrappedWidth) {
			wrappedWidth = available;
			List<FormattedCharSequence> lines = ui.font.split(FormattedText.of(setting.description()), Math.max(40, available));
			wrapped = lines.size() > MAX_DESCRIPTION_LINES ? lines.subList(0, MAX_DESCRIPTION_LINES) : lines;
		}
		return wrapped;
	}

	/** Base height of a setting row: the label line plus the wrapped description. */
	protected int baseHeight(UiContext ui) {
		int lines = descriptionLines(ui).size();
		return lines == 0 ? 18 : 18 + lines * 10;
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
		String label = Draw2D.ellipsize(ui.font, setting.name(), textWidth - 12);
		Draw2D.text(g, ui.font, label, x + PAD, y + 5, ui.c(Theme.TEXT));
		// Changed settings get a reset arrow next to their name, visible on hover or keyboard focus.
		resetIconX = -1;
		if (setting.persistent() && !setting.isDefault() && (hover > 0.3 || keyboardFocus)) {
			resetIconX = x + PAD + ui.font.width(label) + 4;
			boolean overIcon = over(mouseX, mouseY, resetIconX - 2, y + 2, 12, 12);
			Icons.draw(g, Icons.Icon.RESET, resetIconX + 4, y + 9, 3.5f, ui.c(overIcon ? ui.detail : Theme.TEXT_FAINT));
		}
		int lineY = y + 16;
		for (FormattedCharSequence line : descriptionLines(ui)) {
			g.drawString(ui.font, line, x + PAD, lineY, ui.c(Theme.TEXT_FAINT), false);
			lineY += 10;
		}
	}

	private int resetIconX = -1;

	/** Whether the click lands on the reset arrow drawn by {@link #renderLabel}. */
	boolean overResetIcon(double mouseX, double mouseY) {
		return resetIconX >= 0 && over(mouseX, mouseY, resetIconX - 2, y + 2, 12, 12);
	}

	protected boolean over(double mouseX, double mouseY, int rx, int ry, int rw, int rh) {
		return mouseX >= rx && mouseX < rx + rw && mouseY >= ry && mouseY < ry + rh;
	}
}
