package dev.hollow.gui;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Animated pill switch. */
final class Toggle {
	static final int WIDTH = 20;
	static final int HEIGHT = 10;
	private double position;

	Toggle(boolean initial) {
		position = initial ? 1 : 0;
	}

	void render(GuiGraphics g, UiContext ui, int x, int y, boolean on, double hover) {
		position = ui.approach(position, on ? 1 : 0, 0.045);
		float t = (float) position;
		int track = Colors.lerp(Theme.OFF, ui.accent, t);
		if (t > 0.05) {
			Draw2D.glow(g, x, y, WIDTH, HEIGHT, ui.c(Colors.withAlpha(ui.accent, (int) (120 * t))), 3, 1f);
		}
		Draw2D.panel(g, x, y, WIDTH, HEIGHT, ui.c(track));
		int knobX = x + 1 + Math.round(t * (WIDTH - HEIGHT));
		int knobColor = Colors.lerp(0xFFB8B0C4, 0xFFFFFFFF, (float) Math.max(t, hover * 0.5));
		Draw2D.panel(g, knobX, y + 1, HEIGHT - 2, HEIGHT - 2, ui.c(knobColor));
	}
}
