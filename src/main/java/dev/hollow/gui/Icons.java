package dev.hollow.gui;

import dev.hollow.core.module.Category;
import dev.hollow.render.Draw2D;
import net.minecraft.client.gui.GuiGraphics;

/** Small procedural line icons for the sidebar, drawn around a center point at any size. */
final class Icons {
	enum Icon {
		ALL,
		COMBAT,
		AIM,
		BRIDGING,
		MOVEMENT,
		RENDER,
		WORLD,
		EXPLORATION,
		PLAYER,
		PERFORMANCE,
		HUD,
		SETTINGS,
		KEYBINDS,
		PROFILES,
		HUD_EDITOR,
		MENU,
		RESET
	}

	private Icons() {
	}

	static Icon of(Category category) {
		return switch (category) {
			case COMBAT -> Icon.COMBAT;
			case AIM_ASSIST -> Icon.AIM;
			case BRIDGING -> Icon.BRIDGING;
			case MOVEMENT -> Icon.MOVEMENT;
			case RENDER -> Icon.RENDER;
			case WORLD -> Icon.WORLD;
			case EXPLORATION -> Icon.EXPLORATION;
			case PLAYER -> Icon.PLAYER;
			case PERFORMANCE -> Icon.PERFORMANCE;
			case HUD -> Icon.HUD;
			case SETTINGS -> Icon.SETTINGS;
		};
	}

	private static void ring(GuiGraphics g, float cx, float cy, float r, float t, int color) {
		Draw2D.roundedBorder(g, cx - r, cy - r, r * 2, r * 2, r, t, color);
	}

	/** Draws an icon of half-size {@code s} (so the icon spans 2s × 2s). */
	static void draw(GuiGraphics g, Icon icon, float cx, float cy, float s, int color) {
		float t = Math.max(1f, s / 4.5f);
		switch (icon) {
			case ALL -> {
				float q = s * 0.8f;
				float gap = s * 0.25f;
				for (int i = 0; i < 4; i++) {
					float x = cx + (i % 2 == 0 ? -q - gap / 2 : gap / 2);
					float y = cy + (i < 2 ? -q - gap / 2 : gap / 2);
					Draw2D.rounded(g, x, y, q, q, q / 4, color);
				}
			}
			case COMBAT -> {
				Draw2D.line(g, cx - s, cy - s, cx + s * 0.7f, cy + s * 0.7f, t, color);
				Draw2D.line(g, cx + s, cy - s, cx - s * 0.7f, cy + s * 0.7f, t, color);
				Draw2D.line(g, cx + s * 0.2f, cy + s * 0.75f, cx + s * 0.75f, cy + s * 0.2f, t, color);
				Draw2D.line(g, cx - s * 0.2f, cy + s * 0.75f, cx - s * 0.75f, cy + s * 0.2f, t, color);
			}
			case AIM -> {
				ring(g, cx, cy, s * 0.7f, t, color);
				Draw2D.line(g, cx, cy - s, cx, cy - s * 0.35f, t, color);
				Draw2D.line(g, cx, cy + s * 0.35f, cx, cy + s, t, color);
				Draw2D.line(g, cx - s, cy, cx - s * 0.35f, cy, t, color);
				Draw2D.line(g, cx + s * 0.35f, cy, cx + s, cy, t, color);
			}
			case BRIDGING -> {
				float b = s * 0.85f;
				Draw2D.roundedBorder(g, cx - s, cy, b, b, 1, t, color);
				Draw2D.roundedBorder(g, cx - s + b, cy, b, b, 1, t, color);
				Draw2D.roundedBorder(g, cx - s + b, cy - b, b, b, 1, t, color);
			}
			case MOVEMENT -> {
				for (float off : new float[] {-s * 0.55f, s * 0.35f}) {
					Draw2D.line(g, cx + off - s * 0.45f, cy - s * 0.8f, cx + off + s * 0.35f, cy, t, color);
					Draw2D.line(g, cx + off + s * 0.35f, cy, cx + off - s * 0.45f, cy + s * 0.8f, t, color);
				}
			}
			case RENDER -> {
				Draw2D.line(g, cx - s, cy, cx - s * 0.4f, cy - s * 0.55f, t, color);
				Draw2D.line(g, cx - s * 0.4f, cy - s * 0.55f, cx + s * 0.4f, cy - s * 0.55f, t, color);
				Draw2D.line(g, cx + s * 0.4f, cy - s * 0.55f, cx + s, cy, t, color);
				Draw2D.line(g, cx - s, cy, cx - s * 0.4f, cy + s * 0.55f, t, color);
				Draw2D.line(g, cx - s * 0.4f, cy + s * 0.55f, cx + s * 0.4f, cy + s * 0.55f, t, color);
				Draw2D.line(g, cx + s * 0.4f, cy + s * 0.55f, cx + s, cy, t, color);
				Draw2D.circle(g, cx, cy, s * 0.3f, color);
			}
			case WORLD -> {
				ring(g, cx, cy, s, t, color);
				Draw2D.line(g, cx - s, cy, cx + s, cy, t, color);
				Draw2D.roundedBorder(g, cx - s * 0.4f, cy - s, s * 0.8f, s * 2, s * 0.4f, t, color);
			}
			case EXPLORATION -> {
				ring(g, cx, cy, s, t, color);
				Draw2D.line(g, cx, cy - s * 0.7f, cx + s * 0.25f, cy, t, color);
				Draw2D.line(g, cx, cy - s * 0.7f, cx - s * 0.25f, cy, t, color);
				Draw2D.line(g, cx - s * 0.25f, cy, cx, cy + s * 0.7f, t * 0.8f, color);
				Draw2D.line(g, cx + s * 0.25f, cy, cx, cy + s * 0.7f, t * 0.8f, color);
			}
			case PLAYER -> {
				Draw2D.circle(g, cx, cy - s * 0.45f, s * 0.45f, color);
				Draw2D.rounded(g, cx - s * 0.8f, cy + s * 0.15f, s * 1.6f, s * 0.85f, s * 0.4f, color);
			}
			case PERFORMANCE -> {
				Draw2D.line(g, cx + s * 0.35f, cy - s, cx - s * 0.45f, cy + s * 0.1f, t, color);
				Draw2D.line(g, cx - s * 0.45f, cy + s * 0.1f, cx + s * 0.45f, cy - s * 0.1f, t, color);
				Draw2D.line(g, cx + s * 0.45f, cy - s * 0.1f, cx - s * 0.35f, cy + s, t, color);
			}
			case HUD -> {
				Draw2D.roundedBorder(g, cx - s, cy - s * 0.75f, s * 2, s * 1.5f, 2, t, color);
				Draw2D.rounded(g, cx - s * 0.65f, cy - s * 0.4f, s * 0.6f, s * 0.25f, 0.5f, color);
				Draw2D.rounded(g, cx + s * 0.1f, cy + s * 0.15f, s * 0.55f, s * 0.25f, 0.5f, color);
			}
			case SETTINGS -> {
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4;
					float dx = (float) Math.cos(a);
					float dy = (float) Math.sin(a);
					Draw2D.line(g, cx + dx * s * 0.55f, cy + dy * s * 0.55f, cx + dx * s, cy + dy * s, t * 1.3f, color);
				}
				ring(g, cx, cy, s * 0.62f, t, color);
			}
			case KEYBINDS -> {
				Draw2D.roundedBorder(g, cx - s, cy - s * 0.65f, s * 2, s * 1.3f, 2, t, color);
				for (int i = 0; i < 3; i++) {
					Draw2D.rounded(g, cx - s * 0.6f + i * s * 0.5f, cy - s * 0.3f, s * 0.25f, s * 0.25f, 0.5f, color);
				}
				Draw2D.line(g, cx - s * 0.45f, cy + s * 0.3f, cx + s * 0.45f, cy + s * 0.3f, t, color);
			}
			case PROFILES -> {
				Draw2D.roundedBorder(g, cx - s * 0.6f, cy - s, s * 1.6f, s * 1.5f, 2, t, color);
				Draw2D.rounded(g, cx - s, cy - s * 0.5f, s * 1.6f, s * 1.5f, 2, color);
			}
			case HUD_EDITOR -> {
				Draw2D.line(g, cx - s, cy, cx + s, cy, t, color);
				Draw2D.line(g, cx, cy - s, cx, cy + s, t, color);
				float h = s * 0.35f;
				Draw2D.line(g, cx - s, cy, cx - s + h, cy - h, t, color);
				Draw2D.line(g, cx - s, cy, cx - s + h, cy + h, t, color);
				Draw2D.line(g, cx + s, cy, cx + s - h, cy - h, t, color);
				Draw2D.line(g, cx + s, cy, cx + s - h, cy + h, t, color);
				Draw2D.line(g, cx, cy - s, cx - h, cy - s + h, t, color);
				Draw2D.line(g, cx, cy - s, cx + h, cy - s + h, t, color);
				Draw2D.line(g, cx, cy + s, cx - h, cy + s - h, t, color);
				Draw2D.line(g, cx, cy + s, cx + h, cy + s - h, t, color);
			}
			case RESET -> {
				// Three-quarter circle with an arrowhead at its open end.
				int segments = 9;
				float px = cx + s;
				float py = cy;
				for (int i = 1; i <= segments; i++) {
					double a = i * (Math.PI * 1.5) / segments;
					float nx = cx + (float) Math.cos(a) * s;
					float ny = cy + (float) Math.sin(a) * s;
					Draw2D.line(g, px, py, nx, ny, t, color);
					px = nx;
					py = ny;
				}
				Draw2D.line(g, px, py, px + s * 0.6f, py - s * 0.35f, t, color);
				Draw2D.line(g, px, py, px + s * 0.35f, py + s * 0.6f, t, color);
			}
			case MENU -> {
				Draw2D.line(g, cx - s, cy - s * 0.6f, cx + s, cy - s * 0.6f, t, color);
				Draw2D.line(g, cx - s, cy, cx + s, cy, t, color);
				Draw2D.line(g, cx - s, cy + s * 0.6f, cx + s, cy + s * 0.6f, t, color);
			}
		}
	}
}
