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
	private final boolean glow;

	Toggle(boolean initial) {
		this(initial, true);
	}

	/** Toggles in dense grids skip the glow, which costs many draw calls. */
	Toggle(boolean initial, boolean glow) {
		position = initial ? 1 : 0;
		this.glow = glow;
	}

	void render(GuiGraphics g, UiContext ui, int x, int y, boolean on, double hover) {
		position = ui.approach(position, on ? 1 : 0, 0.045);
		float t = (float) position;
		int track = Colors.lerp(Theme.OFF, ui.accent, t);
		if (glow && t > 0.05) {
			Draw2D.glow(g, x, y, WIDTH, HEIGHT, HEIGHT / 2f, ui.c(Colors.withAlpha(ui.accent, (int) (120 * t))), 3, 1f);
		}
		Draw2D.rounded(g, x, y, WIDTH, HEIGHT, HEIGHT / 2f, ui.c(track));
		float knobRadius = HEIGHT / 2f - 1.5f;
		float knobX = x + HEIGHT / 2f + t * (WIDTH - HEIGHT);
		int knobColor = Colors.lerp(0xFFB8B0C4, 0xFFFFFFFF, (float) Math.max(t, hover * 0.5));
		Draw2D.circle(g, knobX, y + HEIGHT / 2f, knobRadius, ui.c(knobColor));
	}
}
