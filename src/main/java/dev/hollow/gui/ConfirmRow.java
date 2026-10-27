package dev.hollow.gui;

import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** A destructive action with a label, an explanation and a button that must be clicked twice. */
final class ConfirmRow extends Row {
	private final String name;
	private final String description;
	private final Confirm confirm;
	private final Runnable action;

	ConfirmRow(String name, String description, String button, Runnable action) {
		this.name = name;
		this.description = description;
		this.confirm = new Confirm(button, "Click again to confirm");
		this.action = action;
	}

	@Override
	boolean matches(String query) {
		return name.toLowerCase(java.util.Locale.ROOT).contains(query) || description.toLowerCase(java.util.Locale.ROOT).contains(query);
	}

	private List<FormattedCharSequence> lines(UiContext ui) {
		if (!ui.descriptions || description.isEmpty()) {
			return List.of();
		}
		return ui.font.split(FormattedText.of(description), Math.max(40, width - PAD * 3 - Pill.width(ui, confirm.label())));
	}

	@Override
	int height(UiContext ui) {
		return 18 + lines(ui).size() * 10;
	}

	private int buttonX(UiContext ui) {
		return x + width - PAD - Pill.width(ui, confirm.label());
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		Draw2D.text(g, ui.font, name, x + PAD, y + 5, ui.c(Theme.TEXT));
		int lineY = y + 16;
		for (FormattedCharSequence line : lines(ui)) {
			g.drawString(ui.font, line, x + PAD, lineY, ui.c(Theme.TEXT_FAINT), false);
			lineY += 10;
		}
		int bx = buttonX(ui);
		boolean hovered = Pill.over(mouseX, mouseY, bx, y + 2, Pill.width(ui, confirm.label()), Pill.HEIGHT);
		Pill.button(g, ui, bx, y + 2, confirm.label(), hovered, confirm.armed() ? Pill.Style.DANGER : Pill.Style.NORMAL);
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && Pill.over(mouseX, mouseY, buttonX(ui), y + 2, Pill.width(ui, confirm.label()), Pill.HEIGHT)) {
			return activate();
		}
		return false;
	}

	@Override
	boolean activate() {
		if (confirm.click()) {
			action.run();
		}
		return true;
	}
}
