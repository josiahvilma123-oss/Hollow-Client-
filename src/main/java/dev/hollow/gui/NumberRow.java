package dev.hollow.gui;

import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

final class NumberRow extends Row {
	private static final int BAR_WIDTH = 100;
	private final NumberSetting setting;
	private boolean dragging;
	private double shown;

	NumberRow(NumberSetting setting) {
		this.setting = setting;
		this.shown = setting.fraction();
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui);
	}

	private int barX() {
		return x + width - PAD - BAR_WIDTH;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		String value = setting.formatted();
		int valueWidth = ui.font.width(value);
		renderLabel(g, ui, mouseX, mouseY, BAR_WIDTH + valueWidth + 8);
		shown = ui.approach(shown, setting.fraction(), dragging ? 0.01 : 0.05);
		int bx = barX();
		int by = y + 8;
		Draw2D.rounded(g, bx, by - 0.5f, BAR_WIDTH, 3, 1.5f, ui.c(Theme.OFF));
		float filled = (float) (shown * BAR_WIDTH);
		Draw2D.rounded(g, bx, by - 0.5f, Math.max(3, filled), 3, 1.5f, ui.c(ui.accent));
		float knob = bx + filled;
		boolean active = dragging || hover > 0.5;
		if (active) {
			Draw2D.glow(g, knob - 3.5f, by - 2.5f, 7, 7, 3.5f, ui.c(Colors.withAlpha(ui.accent, 150)), 3, 1f);
		}
		Draw2D.circle(g, knob, by + 1, 3.5f, ui.c(active ? 0xFFFFFFFF : 0xFFD8D0E4));
		Draw2D.text(g, ui.font, value, bx - valueWidth - 6, y + 5, ui.c(active ? ui.accentLight : Theme.TEXT_DIM));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1) {
			setting.reset();
			return true;
		}
		if (over(mouseX, mouseY, barX() - 4, y, BAR_WIDTH + 8, 18)) {
			dragging = true;
			update(mouseX);
			return true;
		}
		return false;
	}

	@Override
	boolean mouseDragged(double mouseX, double mouseY) {
		if (dragging) {
			update(mouseX);
			return true;
		}
		return false;
	}

	@Override
	void mouseReleased() {
		dragging = false;
	}

	/** Adjusts by one step; used for shift + scroll. */
	void nudge(int direction) {
		double step = setting.step() > 0 ? setting.step() : (setting.max() - setting.min()) / 100;
		setting.set(setting.get() + step * direction);
	}

	private void update(double mouseX) {
		setting.setFraction((mouseX - barX()) / BAR_WIDTH);
	}
}
