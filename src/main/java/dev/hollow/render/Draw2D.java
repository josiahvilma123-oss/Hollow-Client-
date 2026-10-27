package dev.hollow.render;

import dev.hollow.core.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/** 2D drawing helpers shared by the HUD and the Hollow menu. */
public final class Draw2D {
	private Draw2D() {
	}

	/** Default corner radius for panels, in GUI pixels. */
	public static final float RADIUS = 3f;

	/** A filled rectangle with Hollow's default rounded corners. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
		rounded(g, x, y, w, h, RADIUS, color);
	}

	/** A 1px border with Hollow's default rounded corners. */
	public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
		roundedBorder(g, x, y, w, h, RADIUS, 1f, color);
	}

	/** A soft outer glow made of concentric, fading rounded rings. */
	public static void glow(GuiGraphics g, int x, int y, int w, int h, int color, int radius, float strength) {
		glow(g, x, y, w, h, RADIUS, color, radius, strength);
	}

	public static void glow(GuiGraphics g, float x, float y, float w, float h, float cornerRadius, int color, int radius, float strength) {
		for (int i = 1; i <= radius; i++) {
			float t = 1f - (float) i / (radius + 1);
			int alpha = Math.round(Colors.alpha(color) * strength * t * t * 0.55f);
			if (alpha <= 0) {
				continue;
			}
			roundedBorder(g, x - i, y - i, w + i * 2, h + i * 2, cornerRadius + i, 1f, Colors.withAlpha(color, alpha));
		}
	}

	public static void circle(GuiGraphics g, float centerX, float centerY, float radius, int color) {
		rounded(g, centerX - radius, centerY - radius, radius * 2, radius * 2, radius, color);
	}

	/**
	 * Filled rounded rectangle. Drawn in real screen pixels (not GUI pixels) with anti-aliased
	 * corner edges, so corners stay smooth at every GUI scale. Corners come from small cached
	 * textures ({@link Corners}); while a loading overlay is up they are drawn row by row instead.
	 */
	public static void rounded(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) {
			return;
		}
		if (Corners.usable()) {
			Corners.rounded(g, x, y, w, h, radius, guiScale(), color);
			return;
		}
		float scale = guiScale();
		int px = Math.round(x * scale);
		int py = Math.round(y * scale);
		int pw = Math.round((x + w) * scale) - px;
		int ph = Math.round((y + h) * scale) - py;
		if (pw <= 0 || ph <= 0) {
			return;
		}
		float r = Math.min(radius * scale, Math.min(pw, ph) / 2f);
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(1f / scale, 1f / scale);
		if (r < 1f) {
			g.fill(px, py, px + pw, py + ph, color);
			pose.popMatrix();
			return;
		}
		int rows = (int) Math.ceil(r);
		if (ph - rows * 2 > 0) {
			g.fill(px, py + rows, px + pw, py + ph - rows, color);
		}
		for (int row = 0; row < rows; row++) {
			float inset = cornerInset(r, row);
			int solid = (int) Math.ceil(inset);
			float coverage = solid - inset;
			int top = py + row;
			int bottom = py + ph - 1 - row;
			drawSpan(g, px, pw, solid, coverage, top, color);
			if (bottom != top) {
				drawSpan(g, px, pw, solid, coverage, bottom, color);
			}
		}
		pose.popMatrix();
	}

	/** Rounded outline of the given thickness (GUI pixels), anti-aliased on the outer edge. */
	public static void roundedBorder(GuiGraphics g, float x, float y, float w, float h, float radius, float thickness, int color) {
		if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) {
			return;
		}
		if (Corners.usable()) {
			Corners.border(g, x, y, w, h, radius, thickness, guiScale(), color);
			return;
		}
		float scale = guiScale();
		int px = Math.round(x * scale);
		int py = Math.round(y * scale);
		int pw = Math.round((x + w) * scale) - px;
		int ph = Math.round((y + h) * scale) - py;
		int t = Math.max(1, Math.round(thickness * scale));
		if (pw <= t * 2 || ph <= t * 2) {
			rounded(g, x, y, w, h, radius, color);
			return;
		}
		float r = Math.min(radius * scale, Math.min(pw, ph) / 2f);
		float innerR = Math.max(0, r - t);
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(1f / scale, 1f / scale);
		int rows = Math.max(t, (int) Math.ceil(r));
		// Straight sides between the corner regions.
		if (ph - rows * 2 > 0) {
			g.fill(px, py + rows, px + t, py + ph - rows, color);
			g.fill(px + pw - t, py + rows, px + pw, py + ph - rows, color);
		}
		for (int row = 0; row < rows; row++) {
			float outerInset = r >= 1 && row < Math.ceil(r) ? cornerInset(r, row) : 0;
			int outerSolid = (int) Math.ceil(outerInset);
			float coverage = outerSolid - outerInset;
			int innerSolid;
			if (row < t) {
				innerSolid = -1;
			} else {
				int innerRow = row - t;
				float innerInset = innerR >= 1 && innerRow < Math.ceil(innerR) ? cornerInset(innerR, innerRow) : 0;
				innerSolid = t + (int) Math.floor(innerInset);
			}
			for (int y2 : new int[] {py + row, py + ph - 1 - row}) {
				if (innerSolid < 0) {
					drawSpan(g, px, pw, outerSolid, coverage, y2, color);
				} else {
					int left = Math.max(outerSolid, 0);
					if (innerSolid > left) {
						g.fill(px + left, y2, px + innerSolid, y2 + 1, color);
						g.fill(px + pw - innerSolid, y2, px + pw - left, y2 + 1, color);
					}
					if (coverage > 0.02f && outerSolid > 0) {
						int aa = Colors.fade(color, coverage);
						g.fill(px + outerSolid - 1, y2, px + outerSolid, y2 + 1, aa);
						g.fill(px + pw - outerSolid, y2, px + pw - outerSolid + 1, y2 + 1, aa);
					}
				}
				if (ph - 1 - row == row) {
					break;
				}
			}
		}
		pose.popMatrix();
	}

	/** Horizontal distance from the rectangle edge to the circle edge for a corner row. */
	private static float cornerInset(float r, int row) {
		float dy = r - (row + 0.5f);
		return r - (float) Math.sqrt(Math.max(0, r * r - dy * dy));
	}

	private static void drawSpan(GuiGraphics g, int px, int pw, int solid, float coverage, int y, int color) {
		if (pw - solid * 2 > 0) {
			g.fill(px + solid, y, px + pw - solid, y + 1, color);
		}
		if (coverage > 0.02f && solid > 0) {
			int aa = Colors.fade(color, coverage);
			g.fill(px + solid - 1, y, px + solid, y + 1, aa);
			g.fill(px + pw - solid, y, px + pw - solid + 1, y + 1, aa);
		}
	}

	static float guiScale() {
		return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
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
