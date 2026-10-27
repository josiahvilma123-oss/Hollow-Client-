package dev.hollow.gui;

import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

final class EnumRow extends Row {
	private static final int CONTROL_WIDTH = 104;
	private final EnumSetting<?> setting;
	private double flash;

	EnumRow(EnumSetting<?> setting) {
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

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, CONTROL_WIDTH);
		flash = ui.approach(flash, 0, 0.12);
		int cx = x + width - PAD - CONTROL_WIDTH;
		int cy = y + 2;
		boolean hovered = over(mouseX, mouseY, cx, cy, CONTROL_WIDTH, 14);
		Draw2D.panel(g, cx, cy, CONTROL_WIDTH, 14, ui.c(hovered ? Theme.CARD_HOVER : Theme.CARD));
		Draw2D.border(g, cx, cy, CONTROL_WIDTH, 14, ui.c(dev.hollow.core.util.Colors.lerp(Theme.BORDER, ui.accent, (float) Math.max(flash, hovered ? 0.5 : 0))));
		Draw2D.text(g, ui.font, "‹", cx + 4, cy + 3, ui.c(Theme.TEXT_FAINT));
		Draw2D.text(g, ui.font, "›", cx + CONTROL_WIDTH - 8, cy + 3, ui.c(Theme.TEXT_FAINT));
		String value = Draw2D.ellipsize(ui.font, setting.displayValue(), CONTROL_WIDTH - 20);
		Draw2D.centered(g, ui.font, value, cx + CONTROL_WIDTH / 2, cy + 3, ui.c(ui.accentLight));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		int cx = x + width - PAD - CONTROL_WIDTH;
		boolean left = mouseX < cx + CONTROL_WIDTH / 2.0 && mouseX >= cx;
		setting.cycle(button == 1 || left && button == 0 && mouseX < cx + 14 ? -1 : 1);
		flash = 1;
		return true;
	}

	@Override
	boolean activate() {
		return adjust(1);
	}

	@Override
	boolean adjust(int direction) {
		setting.cycle(direction);
		flash = 1;
		return true;
	}
}
