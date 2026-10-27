package dev.hollow.gui;

import dev.hollow.render.Theme;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** A paragraph of read-only text (explanations, live status). */
final class InfoRow extends Row {
	private final Supplier<String> text;
	private final int color;
	private String wrappedText;
	private int wrappedFor = -1;
	private List<FormattedCharSequence> lines = List.of();

	InfoRow(Supplier<String> text, int color) {
		this.text = text;
		this.color = color;
	}

	InfoRow(String text) {
		this(() -> text, Theme.TEXT_DIM);
	}

	private List<FormattedCharSequence> lines(UiContext ui) {
		String current = text.get();
		if (!current.equals(wrappedText) || width != wrappedFor) {
			wrappedText = current;
			wrappedFor = width;
			lines = current.isEmpty() ? List.of() : ui.font.split(FormattedText.of(current), Math.max(40, width - PAD * 2));
		}
		return lines;
	}

	@Override
	boolean visible() {
		return !text.get().isEmpty();
	}

	@Override
	boolean focusable() {
		return false;
	}

	@Override
	int height(UiContext ui) {
		return lines(ui).size() * 10 + 6;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		int lineY = y + 3;
		for (FormattedCharSequence line : lines(ui)) {
			g.drawString(ui.font, line, x + PAD, lineY, ui.c(color), false);
			lineY += 10;
		}
	}
}
