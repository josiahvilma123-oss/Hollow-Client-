package dev.hollow.gui;

import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Editable list of ids shown as removable chips with an input line below. */
final class ListRow extends Row {
	private static final int CHIP_HEIGHT = 12;
	private static final int GAP = 4;
	private static final int ADD_WIDTH = 34;
	private final StringListSetting setting;
	private final TextInput input;
	private double error;

	ListRow(StringListSetting setting) {
		this.setting = setting;
		this.input = new TextInput(setting.placeholder(), 96).validator(setting::isValidEntry);
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	private int chipsHeight(UiContext ui) {
		List<String> entries = setting.get();
		if (entries.isEmpty()) {
			return CHIP_HEIGHT;
		}
		int lineWidth = width - PAD * 2;
		int cx = 0;
		int lines = 1;
		for (String entry : entries) {
			int w = chipWidth(ui, entry);
			if (cx > 0 && cx + w > lineWidth) {
				lines++;
				cx = 0;
			}
			cx += w + GAP;
		}
		return lines * (CHIP_HEIGHT + GAP) - GAP;
	}

	private static int chipWidth(UiContext ui, String entry) {
		return ui.font.width(entry) + 16;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui) + chipsHeight(ui) + 6 + 16 + 4;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, 0);
		error = ui.approach(error, 0, 0.15);
		int top = y + baseHeight(ui);
		List<String> entries = setting.get();
		if (entries.isEmpty()) {
			Draw2D.text(g, ui.font, "Empty", x + PAD, top + 2, ui.c(Theme.TEXT_FAINT));
		}
		int cx = x + PAD;
		int cy = top;
		int right = x + width - PAD;
		for (String entry : entries) {
			int w = chipWidth(ui, entry);
			if (cx > x + PAD && cx + w > right) {
				cx = x + PAD;
				cy += CHIP_HEIGHT + GAP;
			}
			boolean hovered = over(mouseX, mouseY, cx, cy, w, CHIP_HEIGHT);
			Draw2D.panel(g, cx, cy, w, CHIP_HEIGHT, ui.c(hovered ? Theme.CARD_HOVER : Theme.CARD));
			Draw2D.border(g, cx, cy, w, CHIP_HEIGHT, ui.c(hovered ? ui.accent : Theme.BORDER));
			Draw2D.text(g, ui.font, entry, cx + 4, cy + 2, ui.c(Theme.TEXT));
			Draw2D.text(g, ui.font, "×", cx + w - 9, cy + 2, ui.c(hovered ? Theme.DANGER : Theme.TEXT_FAINT));
			cx += w + GAP;
		}
		int inputY = top + chipsHeight(ui) + 6;
		int inputWidth = width - PAD * 2 - ADD_WIDTH - 4;
		input.render(g, ui, x + PAD, inputY, inputWidth, 16);
		if (error > 0.02) {
			Draw2D.border(g, x + PAD, inputY, inputWidth, 16, ui.c(Colors.withAlpha(Theme.DANGER, (int) (error * 255))));
		}
		int bx = x + width - PAD - ADD_WIDTH;
		boolean hovered = over(mouseX, mouseY, bx, inputY, ADD_WIDTH, 16);
		Draw2D.panel(g, bx, inputY, ADD_WIDTH, 16, ui.c(hovered ? ui.accent : ui.accentDarkOr()));
		Draw2D.centered(g, ui.font, "Add", bx + ADD_WIDTH / 2, inputY + 4, ui.c(Theme.TEXT));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		int top = y + baseHeight(ui);
		int cx = x + PAD;
		int cy = top;
		int right = x + width - PAD;
		for (String entry : setting.get()) {
			int w = chipWidth(ui, entry);
			if (cx > x + PAD && cx + w > right) {
				cx = x + PAD;
				cy += CHIP_HEIGHT + GAP;
			}
			if (over(mouseX, mouseY, cx, cy, w, CHIP_HEIGHT)) {
				setting.remove(entry);
				return true;
			}
			cx += w + GAP;
		}
		int inputY = top + chipsHeight(ui) + 6;
		int bx = x + width - PAD - ADD_WIDTH;
		if (over(mouseX, mouseY, bx, inputY, ADD_WIDTH, 16)) {
			submit();
			return true;
		}
		input.focus(over(mouseX, mouseY, x + PAD, inputY, width - PAD * 2 - ADD_WIDTH - 4, 16));
		return true;
	}

	private void submit() {
		if (input.text().isBlank()) {
			return;
		}
		if (setting.add(input.text())) {
			input.setText("");
		} else {
			error = 1;
		}
	}

	@Override
	boolean capturesKeyboard() {
		return input.focused();
	}

	@Override
	boolean cancelInput() {
		if (input.focused()) {
			input.focus(false);
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
			submit();
			return true;
		}
		return input.keyPressed(event);
	}

	@Override
	boolean charTyped(CharacterEvent event) {
		return input.charTyped(event);
	}
}
