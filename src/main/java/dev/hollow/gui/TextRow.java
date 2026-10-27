package dev.hollow.gui;

import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.TextSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** A single-line text setting. Input is applied on Enter or when focus leaves; invalid text is outlined red and not applied. */
final class TextRow extends Row {
	private final TextSetting setting;
	private final TextInput input;

	TextRow(TextSetting setting) {
		this.setting = setting;
		this.input = new TextInput(setting.placeholder(), setting.maxLength()).validator(setting::isValid);
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui) + 20;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, 0);
		if (!input.focused() && !input.text().equals(setting.get())) {
			input.setText(setting.get());
		}
		input.render(g, ui, x + PAD, y + baseHeight(ui), width - PAD * 2, 16);
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean onInput = over(mouseX, mouseY, x + PAD, y + baseHeight(ui), width - PAD * 2, 16);
		if (!onInput && input.focused()) {
			commit();
		}
		input.focus(onInput);
		return true;
	}

	private void commit() {
		if (setting.isValid(input.text())) {
			setting.set(input.text());
		}
		input.setText(setting.get());
		input.focus(false);
	}

	@Override
	boolean capturesKeyboard() {
		return input.focused();
	}

	@Override
	boolean cancelInput() {
		if (input.focused()) {
			commit();
			return true;
		}
		return false;
	}

	@Override
	boolean keyPressed(KeyEvent event) {
		if (!input.focused()) {
			return false;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			commit();
			return true;
		}
		return input.keyPressed(event);
	}

	@Override
	boolean charTyped(CharacterEvent event) {
		return input.charTyped(event);
	}

	@Override
	boolean activate() {
		input.focus(true);
		return true;
	}
}
