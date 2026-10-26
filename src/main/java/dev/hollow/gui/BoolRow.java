package dev.hollow.gui;

import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;

final class BoolRow extends Row {
	private final BoolSetting setting;
	private final Toggle toggle;

	BoolRow(BoolSetting setting) {
		this.setting = setting;
		this.toggle = new Toggle(setting.get());
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
		renderLabel(g, ui, mouseX, mouseY, Toggle.WIDTH);
		toggle.render(g, ui, x + width - PAD - Toggle.WIDTH, y + 4, setting.get(), hover);
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		setting.toggle();
		return true;
	}
}
