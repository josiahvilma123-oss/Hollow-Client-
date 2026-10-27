package dev.hollow.gui;

import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Color swatch that expands into an inline HSV picker with alpha and hex entry. */
final class ColorRow extends Row {
	private static final int SV_SIZE = 64;
	private static final int BAR = 9;
	private static final int PICKER_HEIGHT = SV_SIZE + 12;
	private final ColorSetting setting;
	private final TextInput hex = new TextInput("#AARRGGBB", 9).validator(s -> Colors.parseHex(s) != null);
	private boolean open;
	private double openAmount;
	private float hue;
	private float saturation;
	private float value;
	private Drag drag = Drag.NONE;

	private enum Drag { NONE, SV, HUE, ALPHA }

	ColorRow(ColorSetting setting) {
		this.setting = setting;
		syncFromSetting();
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	private void syncFromSetting() {
		float[] hsv = Colors.toHsv(setting.argb());
		if (hsv[1] > 0.01f && hsv[2] > 0.01f) {
			hue = hsv[0];
		}
		saturation = hsv[1];
		value = hsv[2];
		hex.setText(Colors.toHex(setting.argb()));
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui) + (int) Math.round(openAmount * PICKER_HEIGHT);
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		openAmount = ui.approach(openAmount, open ? 1 : 0, 0.05);
		int swatchW = 26;
		String hexText = Colors.toHex(setting.argb());
		renderLabel(g, ui, mouseX, mouseY, swatchW + ui.font.width(hexText) + 8);
		int sx = x + width - PAD - swatchW;
		int sy = y + 3;
		drawChecker(g, ui, sx, sy, swatchW, 11);
		Draw2D.panel(g, sx, sy, swatchW, 11, ui.c(setting.argb()));
		Draw2D.border(g, sx, sy, swatchW, 11, ui.c(open ? ui.accent : Theme.BORDER));
		Draw2D.text(g, ui.font, hexText, sx - ui.font.width(hexText) - 6, y + 5, ui.c(Theme.TEXT_DIM));
		if (openAmount < 0.05) {
			return;
		}
		int top = y + baseHeight(ui);
		int visible = (int) Math.round(openAmount * PICKER_HEIGHT);
		g.enableScissor(x, top, x + width, top + visible);
		renderPicker(g, ui, x + PAD, top + 4);
		g.disableScissor();
	}

	private void renderPicker(GuiGraphics g, UiContext ui, int px, int py) {
		// Saturation/value square drawn as vertical strips blended to black.
		for (int i = 0; i < SV_SIZE; i += 2) {
			float s = i / (float) (SV_SIZE - 1);
			int top = Colors.hsv(hue, s, 1, 255);
			g.fillGradient(px + i, py, px + i + 2, py + SV_SIZE, ui.c(top), ui.c(0xFF000000));
		}
		Draw2D.border(g, px - 1, py - 1, SV_SIZE + 2, SV_SIZE + 2, ui.c(Theme.BORDER));
		int markerX = px + Math.round(saturation * (SV_SIZE - 1));
		int markerY = py + Math.round((1 - value) * (SV_SIZE - 1));
		Draw2D.border(g, markerX - 2, markerY - 2, 5, 5, ui.c(0xFFFFFFFF));

		int hx = px + SV_SIZE + 6;
		for (int i = 0; i < SV_SIZE; i++) {
			g.fill(hx, py + i, hx + BAR, py + i + 1, ui.c(Colors.hsv(i / (float) SV_SIZE, 1, 1, 255)));
		}
		Draw2D.border(g, hx - 1, py - 1, BAR + 2, SV_SIZE + 2, ui.c(Theme.BORDER));
		int hueY = py + Math.round(hue * (SV_SIZE - 1));
		g.fill(hx - 2, hueY, hx + BAR + 2, hueY + 1, ui.c(0xFFFFFFFF));

		int ax = hx + BAR + 6;
		if (setting.allowsAlpha()) {
			drawChecker(g, ui, ax, py, BAR, SV_SIZE);
			int opaque = setting.argb() | 0xFF000000;
			g.fillGradient(ax, py, ax + BAR, py + SV_SIZE, ui.c(opaque), ui.c(opaque & 0x00FFFFFF));
			Draw2D.border(g, ax - 1, py - 1, BAR + 2, SV_SIZE + 2, ui.c(Theme.BORDER));
			int alphaY = py + Math.round((1 - Colors.alpha(setting.argb()) / 255f) * (SV_SIZE - 1));
			g.fill(ax - 2, alphaY, ax + BAR + 2, alphaY + 1, ui.c(0xFFFFFFFF));
			ax += BAR + 8;
		}
		int fieldWidth = Math.min(90, x + width - PAD - ax);
		if (fieldWidth > 40) {
			Draw2D.text(g, ui.font, "Hex", ax, py + 1, ui.c(Theme.TEXT_FAINT));
			hex.render(g, ui, ax, py + 11, fieldWidth, 14);
			Draw2D.text(g, ui.font, "Right-click", ax, py + 32, ui.c(Theme.TEXT_FAINT));
			Draw2D.text(g, ui.font, "swatch: reset", ax, py + 42, ui.c(Theme.TEXT_FAINT));
		}
	}

	private static void drawChecker(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		for (int cy = 0; cy < h; cy += 4) {
			for (int cx = 0; cx < w; cx += 4) {
				int shade = (cx / 4 + cy / 4) % 2 == 0 ? 0xFF3A3540 : 0xFF24202A;
				g.fill(x + cx, y + cy, Math.min(x + w, x + cx + 4), Math.min(y + h, y + cy + 4), ui.c(shade));
			}
		}
	}

	private int pickerX() {
		return x + PAD;
	}

	private int pickerY(UiContext ui) {
		return y + baseHeight(ui) + 4;
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (over(mouseX, mouseY, x, y, width, baseHeight(ui))) {
			if (button == 1) {
				setting.reset();
				syncFromSetting();
			} else {
				open = !open;
				if (open) {
					syncFromSetting();
				} else {
					hex.focus(false);
				}
			}
			return true;
		}
		if (!open) {
			return mouseY < y + baseHeight(ui);
		}
		int px = pickerX();
		int py = pickerY(ui);
		int hx = px + SV_SIZE + 6;
		int ax = hx + BAR + 6;
		hex.focus(false);
		if (over(mouseX, mouseY, px, py, SV_SIZE, SV_SIZE)) {
			drag = Drag.SV;
		} else if (over(mouseX, mouseY, hx - 2, py, BAR + 4, SV_SIZE)) {
			drag = Drag.HUE;
		} else if (setting.allowsAlpha() && over(mouseX, mouseY, ax - 2, py, BAR + 4, SV_SIZE)) {
			drag = Drag.ALPHA;
		} else {
			int fx = setting.allowsAlpha() ? ax + BAR + 8 : ax;
			if (over(mouseX, mouseY, fx, py + 11, 90, 14)) {
				hex.focus(true);
			}
			return true;
		}
		applyDrag(mouseX, mouseY);
		return true;
	}

	@Override
	boolean mouseDragged(double mouseX, double mouseY) {
		if (drag == Drag.NONE) {
			return false;
		}
		applyDrag(mouseX, mouseY);
		return true;
	}

	@Override
	void mouseReleased() {
		drag = Drag.NONE;
	}

	private void applyDrag(double mouseX, double mouseY) {
		int px = pickerX();
		int py = pickerY(ui);
		float fy = (float) Math.max(0, Math.min(1, (mouseY - py) / (SV_SIZE - 1)));
		int alpha = Colors.alpha(setting.argb());
		switch (drag) {
			case SV -> {
				saturation = (float) Math.max(0, Math.min(1, (mouseX - px) / (SV_SIZE - 1)));
				value = 1 - fy;
			}
			case HUE -> hue = Math.min(0.999f, fy);
			case ALPHA -> alpha = Math.round((1 - fy) * 255);
			default -> {
				return;
			}
		}
		setting.set(Colors.hsv(hue, saturation, value, setting.allowsAlpha() ? alpha : 255));
		hex.setText(Colors.toHex(setting.argb()));
	}

	@Override
	boolean capturesKeyboard() {
		return hex.focused();
	}

	@Override
	boolean cancelInput() {
		if (hex.focused()) {
			hex.focus(false);
			hex.setText(Colors.toHex(setting.argb()));
			return true;
		}
		return false;
	}

	@Override
	boolean keyPressed(KeyEvent event) {
		if (!hex.focused()) {
			return false;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			Integer parsed = Colors.parseHex(hex.text());
			if (parsed != null) {
				setting.set(parsed);
				syncFromSetting();
			}
			hex.focus(false);
			return true;
		}
		boolean handled = hex.keyPressed(event);
		commitIfValid();
		return handled;
	}

	@Override
	boolean charTyped(CharacterEvent event) {
		boolean handled = hex.charTyped(event);
		commitIfValid();
		return handled;
	}

	private void commitIfValid() {
		Integer parsed = Colors.parseHex(hex.text());
		if (parsed != null) {
			setting.set(parsed);
			float[] hsv = Colors.toHsv(setting.argb());
			if (hsv[1] > 0.01f && hsv[2] > 0.01f) {
				hue = hsv[0];
			}
			saturation = hsv[1];
			value = hsv[2];
		}
	}


	@Override
	boolean activate() {
		open = !open;
		if (open) {
			syncFromSetting();
		} else {
			hex.focus(false);
		}
		return true;
	}
}
