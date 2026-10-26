package dev.hollow.gui;

import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

final class ActionRow extends Row {
	private final ActionSetting setting;
	private double pressed;

	ActionRow(ActionSetting setting) {
		this.setting = setting;
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui);
	}

	private int buttonWidth(UiContext ui) {
		return ui.font.width(setting.buttonLabel()) + 16;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		int bw = buttonWidth(ui);
		renderLabel(g, ui, mouseX, mouseY, bw);
		pressed = ui.approach(pressed, 0, 0.1);
		int bx = x + width - PAD - bw;
		int by = y + 2;
		boolean hovered = over(mouseX, mouseY, bx, by, bw, 14);
		int fill = dev.hollow.core.util.Colors.lerp(Theme.CARD, ui.accentDarkOr(), (float) Math.max(pressed, hovered ? 0.6 : 0.25));
		if (hovered) {
			Draw2D.glow(g, bx, by, bw, 14, ui.c(dev.hollow.core.util.Colors.withAlpha(ui.accent, 110)), 3, 1f);
		}
		Draw2D.panel(g, bx, by, bw, 14, ui.c(fill));
		Draw2D.border(g, bx, by, bw, 14, ui.c(hovered ? ui.accent : Theme.BORDER));
		Draw2D.centered(g, ui.font, setting.buttonLabel(), bx + bw / 2, by + 3, ui.c(Theme.TEXT));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		setting.run();
		pressed = 1;
		return true;
	}
}
