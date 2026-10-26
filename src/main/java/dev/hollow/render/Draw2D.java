package dev.hollow.render;

import dev.hollow.core.util.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/** 2D drawing helpers shared by the HUD and the Hollow menu. */
public final class Draw2D {
	private Draw2D() {
	}

	/** A filled rectangle with softened (1px cut) corners. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w <= 2 || h <= 2) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x + 1, y, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** A 1px border with softened corners. */
	public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + 1, y, x + w - 1, y + 1, color);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** A soft outer glow made of concentric, fading borders. */
	public static void glow(GuiGraphics g, int x, int y, int w, int h, int color, int radius, float strength) {
		for (int i = 1; i <= radius; i++) {
			float t = 1f - (float) i / (radius + 1);
			int alpha = Math.round(Colors.alpha(color) * strength * t * t * 0.55f);
			if (alpha <= 0) {
				continue;
			}
			border(g, x - i, y - i, w + i * 2, h + i * 2, Colors.withAlpha(color, alpha));
		}
	}

	public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, false);
	}

	public static void textShadow(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, true);
	}

	public static void centered(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
		g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
	}

	/** Draws text with extra letter spacing, used for the HOLLOW wordmark and headings. */
	public static int spaced(GuiGraphics g, Font font, String text, int x, int y, int color, int spacing) {
		int cursor = x;
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			g.drawString(font, ch, cursor, y, color, false);
			cursor += font.width(ch) + spacing;
		}
		return cursor - x - spacing;
	}

	public static int spacedWidth(Font font, String text, int spacing) {
		int width = 0;
		for (int i = 0; i < text.length(); i++) {
			width += font.width(String.valueOf(text.charAt(i))) + spacing;
		}
		return Math.max(0, width - spacing);
	}

	/** Trims text to fit {@code maxWidth}, adding an ellipsis. */
	public static String ellipsize(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "…";
		int available = maxWidth - font.width(ellipsis);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			if (font.width(builder.toString() + text.charAt(i)) > available) {
				break;
			}
			builder.append(text.charAt(i));
		}
		return builder + ellipsis;
	}

	/** An anti-aliasing-free thick line between two points, drawn as a rotated rectangle. */
	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 0.01f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x1, y1);
		pose.rotate((float) Math.atan2(dy, dx));
		pose.translate(0, -thickness / 2f);
		pose.scale(length, thickness);
		g.fill(0, 0, 1, 1, color);
		pose.popMatrix();
	}

	/**
	 * Hollow's scythe mark, drawn procedurally so it scales cleanly: a gently curved snath with a
	 * grip, and a crescent blade sweeping down to the left. Matches the mod icon.
	 */
	public static void scythe(GuiGraphics g, float x, float y, float size, int color) {
		float s = size / 16f;
		float width = Math.max(1f, 0.95f * s);
		curve(g, x + 5 * s, y + 15.6f * s, x + 9.8f * s, y + 9.2f * s, x + 11.6f * s, y + 1.4f * s, width, width, color, 8);
		line(g, x + 8.9f * s, y + 8.4f * s, x + 7.2f * s, y + 7.5f * s, Math.max(1f, 0.8f * s), color);
		// Blade: thick at the heel, tapering to the tip.
		curve(g, x + 11.6f * s, y + 1.9f * s, x + 5.2f * s, y + 0.2f * s, x + 0.4f * s, y + 7.6f * s,
			Math.max(1.2f, 2.1f * s), Math.max(0.75f, 0.4f * s), color, 14);
	}

	private static void curve(GuiGraphics g, float x0, float y0, float cx, float cy, float x2, float y2, float startWidth, float endWidth,
			int color, int segments) {
		float previousX = x0;
		float previousY = y0;
		for (int i = 1; i <= segments; i++) {
			float t = (float) i / segments;
			float u = 1 - t;
			float px = u * u * x0 + 2 * u * t * cx + t * t * x2;
			float py = u * u * y0 + 2 * u * t * cy + t * t * y2;
			line(g, previousX, previousY, px, py, startWidth + (endWidth - startWidth) * t, color);
			previousX = px;
			previousY = py;
		}
	}
}
