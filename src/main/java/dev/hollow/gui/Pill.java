package dev.hollow.gui;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Small rounded buttons and status pills shared by the menu's pages. */
final class Pill {
	static final int HEIGHT = 14;

	enum Style {
		NORMAL,
		PRIMARY,
		DANGER
	}

	private Pill() {
	}

	static int width(UiContext ui, String label) {
		return ui.font.width(label) + 14;
	}

	/** Draws a clickable pill button and returns its width. */
	static int button(GuiGraphics g, UiContext ui, int x, int y, String label, boolean hovered, Style style) {
		int w = width(ui, label);
		int tone = switch (style) {
			case NORMAL -> ui.accentDarkOr();
			case PRIMARY -> ui.accent;
			case DANGER -> Theme.DANGER;
		};
		int fill = Colors.lerp(Theme.CARD, tone, hovered ? 0.65f : style == Style.NORMAL ? 0.2f : 0.4f);
		if (hovered) {
			Draw2D.glow(g, x, y, w, HEIGHT, HEIGHT / 2f, ui.c(Colors.withAlpha(tone, 110)), 3, 1f);
		}
		Draw2D.rounded(g, x, y, w, HEIGHT, HEIGHT / 2f, ui.c(fill));
		Draw2D.roundedBorder(g, x, y, w, HEIGHT, HEIGHT / 2f, 1, ui.c(hovered ? tone : Theme.BORDER));
		Draw2D.centered(g, ui.font, label, x + w / 2, y + 3, ui.c(Theme.TEXT));
		return w;
	}

	/** A non-interactive status label such as ON or BLOCKED, drawn at 3/4 text size; returns its width. */
	static int status(GuiGraphics g, UiContext ui, int x, int y, String label, int color) {
		int w = Math.round(ui.font.width(label) * 0.75f) + 7;
		Draw2D.rounded(g, x, y, w, 9, 2, ui.c(Colors.withAlpha(color, 70)));
		g.pose().pushMatrix();
		g.pose().translate(x + 4, y + 2);
		g.pose().scale(0.75f, 0.75f);
		Draw2D.text(g, ui.font, label, 0, 0, ui.c(Colors.lerp(color, 0xFFFFFFFF, 0.35f)));
		g.pose().popMatrix();
		return w;
	}

	static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}
}
