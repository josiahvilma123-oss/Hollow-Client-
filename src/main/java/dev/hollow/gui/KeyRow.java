package dev.hollow.gui;

import dev.hollow.core.input.Keybind;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Colors;
import dev.hollow.input.KeyInput;
import dev.hollow.input.KeybindConflicts;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind editor. Click, then press a key (with Ctrl/Shift/Alt for a combination) or a mouse
 * button other than left-click. Backspace unbinds, Esc cancels, right-click restores the default.
 * Conflicts with other Holo binds and Minecraft controls are listed below the control.
 */
final class KeyRow extends Row {
	private static final int CONTROL_WIDTH = 104;
	private final KeySetting setting;
	private boolean listening;
	private Keybind conflictsFor;
	private List<String> conflicts = List.of();
	private long conflictsCheckedAt;

	KeyRow(KeySetting setting) {
		this.setting = setting;
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	private List<String> conflicts() {
		long now = System.currentTimeMillis();
		if (!setting.get().equals(conflictsFor) || now - conflictsCheckedAt > 1000) {
			conflictsFor = setting.get();
			conflictsCheckedAt = now;
			conflicts = KeybindConflicts.of(setting, setting.get());
		}
		return conflicts;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui) + (conflicts().isEmpty() ? 0 : 11);
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, CONTROL_WIDTH);
		int cx = x + width - PAD - CONTROL_WIDTH;
		int cy = y + 2;
		boolean pulse = listening && System.currentTimeMillis() / 400 % 2 == 0;
		Draw2D.rounded(g, cx, cy, CONTROL_WIDTH, 14, 4, ui.c(listening ? Theme.CARD_HOVER : Theme.CARD));
		Draw2D.roundedBorder(g, cx, cy, CONTROL_WIDTH, 14, 4, 1, ui.c(listening ? (pulse ? ui.accentLight : ui.accent) : Theme.BORDER));
		String label = listening ? "Press a key…" : KeyInput.describe(setting.get());
		Draw2D.centered(g, ui.font, Draw2D.ellipsize(ui.font, label, CONTROL_WIDTH - 8), cx + CONTROL_WIDTH / 2, cy + 3,
			ui.c(listening ? ui.accentLight : setting.isBound() ? Theme.TEXT : Theme.TEXT_FAINT));
		List<String> found = conflicts();
		if (!found.isEmpty()) {
			String text = "⚠ Also used by " + String.join(", ", found);
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, text, width - PAD * 2), x + PAD, y + height(ui) - 11,
				ui.c(Colors.lerp(Theme.DANGER, 0xFFFFD27A, 0.4f)));
		}
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (listening && button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			setting.set(Keybind.mouse(button));
			listening = false;
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			setting.reset();
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
		} else if (key != GLFW.GLFW_KEY_UNKNOWN) {
			int mods = KeyInput.heldModifiers(Minecraft.getInstance().getWindow()) & ~KeyInput.modifierOf(key);
			setting.set(Keybind.key(key, mods));
			listening = false;
		}
		return true;
	}

	@Override
	boolean activate() {
		listening = true;
		return true;
	}
}
