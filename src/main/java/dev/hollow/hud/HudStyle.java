package dev.hollow.hud;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shared look of Holo HUD panels: dark rounded card, thin accent border, gold title. */
public final class HudStyle {
	public static final int GOLD = 0xFFE6B54A;

	private HudStyle() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	public static void panel(GuiGraphics g, int w, int h) {
		Draw2D.rounded(g, 0, 0, w, h, 4, 0xC80B0910);
		Draw2D.roundedBorder(g, 0, 0, w, h, 4, 1, Colors.withAlpha(Theme.accent(), 90));
	}

	public static void title(GuiGraphics g, String text, int x, int y) {
		Draw2D.spaced(g, font(), text, x, y, GOLD, 1);
	}

	public static void line(GuiGraphics g, String label, String value, int x, int y, int width, int valueColor) {
		Font font = font();
		Draw2D.text(g, font, label, x, y, Theme.TEXT_DIM);
		Draw2D.text(g, font, value, x + width - font.width(value), y, valueColor);
	}
}
