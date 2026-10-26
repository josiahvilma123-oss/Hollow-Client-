package dev.hollow.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

final class KeyRow extends Row {
	private static final int CONTROL_WIDTH = 84;
	private final KeySetting setting;
	private boolean listening;

	KeyRow(KeySetting setting) {
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

	static String keyName(int key) {
		if (key == KeySetting.NONE) {
			return "None";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, CONTROL_WIDTH);
		int cx = x + width - PAD - CONTROL_WIDTH;
		int cy = y + 2;
		boolean pulse = listening && System.currentTimeMillis() / 400 % 2 == 0;
		Draw2D.panel(g, cx, cy, CONTROL_WIDTH, 14, ui.c(listening ? Theme.CARD_HOVER : Theme.CARD));
		Draw2D.border(g, cx, cy, CONTROL_WIDTH, 14, ui.c(listening ? (pulse ? ui.accentLight : ui.accent) : Theme.BORDER));
		String label = listening ? "Press a key…" : keyName(setting.get());
		Draw2D.centered(g, ui.font, Draw2D.ellipsize(ui.font, label, CONTROL_WIDTH - 8), cx + CONTROL_WIDTH / 2, cy + 3,
			ui.c(listening ? ui.accentLight : setting.isBound() ? Theme.TEXT : Theme.TEXT_FAINT));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1) {
			setting.unbind();
			listening = false;
			return true;
		}
		listening = !listening;
		return true;
	}

	@Override
	boolean capturesKeyboard() {
		return listening;
	}

	@Override
	boolean cancelInput() {
		boolean was = listening;
		listening = false;
		return was;
	}

	@Override
	boolean keyPressed(KeyEvent event) {
		if (!listening) {
			return false;
		}
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			listening = false;
		} else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
			setting.unbind();
			listening = false;
		} else {
			setting.set(key);
			listening = false;
		}
		return true;
	}
}
