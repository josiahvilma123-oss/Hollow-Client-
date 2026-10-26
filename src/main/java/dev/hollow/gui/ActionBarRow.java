package dev.hollow.gui;

import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** A label with a wrapping row of pill buttons below it. */
final class ActionBarRow extends Row {
	private static final int BUTTON_HEIGHT = 15;
	private static final int GAP = 4;
	private final ActionBarSetting setting;
	private final double[] pressed;

	ActionBarRow(ActionBarSetting setting) {
		this.setting = setting;
		this.pressed = new double[setting.buttons().size()];
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	private static int buttonWidth(UiContext ui, ActionBarSetting.Button button) {
		return ui.font.width(button.label()) + 14;
	}

	private int lines(UiContext ui) {
		int lines = 1;
		int cx = 0;
		int available = width - PAD * 2;
		for (ActionBarSetting.Button button : setting.buttons()) {
			int w = buttonWidth(ui, button);
			if (cx > 0 && cx + w > available) {
				lines++;
				cx = 0;
			}
			cx += w + GAP;
		}
		return lines;
	}

	@Override
	int height(UiContext ui) {
		return baseHeight(ui) + lines(ui) * (BUTTON_HEIGHT + GAP) + 2;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		renderLabel(g, ui, mouseX, mouseY, 0);
		List<ActionBarSetting.Button> buttons = setting.buttons();
		int cx = x + PAD;
		int cy = y + baseHeight(ui);
		for (int i = 0; i < buttons.size(); i++) {
			ActionBarSetting.Button button = buttons.get(i);
			int w = buttonWidth(ui, button);
			if (cx > x + PAD && cx + w > x + width - PAD) {
				cx = x + PAD;
				cy += BUTTON_HEIGHT + GAP;
			}
			pressed[i] = ui.approach(pressed[i], 0, 0.1);
			boolean hovered = over(mouseX, mouseY, cx, cy, w, BUTTON_HEIGHT);
			int fill = Colors.lerp(Theme.CARD, ui.accentDarkOr(), (float) Math.max(pressed[i], hovered ? 0.6 : 0.2));
			if (hovered) {
				Draw2D.glow(g, cx, cy, w, BUTTON_HEIGHT, BUTTON_HEIGHT / 2f, ui.c(Colors.withAlpha(ui.accent, 110)), 3, 1f);
			}
			Draw2D.rounded(g, cx, cy, w, BUTTON_HEIGHT, BUTTON_HEIGHT / 2f, ui.c(fill));
			Draw2D.roundedBorder(g, cx, cy, w, BUTTON_HEIGHT, BUTTON_HEIGHT / 2f, 1, ui.c(hovered ? ui.accent : Theme.BORDER));
			Draw2D.centered(g, ui.font, button.label(), cx + w / 2, cy + 4, ui.c(Theme.TEXT));
			cx += w + GAP;
		}
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		List<ActionBarSetting.Button> buttons = setting.buttons();
		int cx = x + PAD;
		int cy = y + baseHeight(ui);
		for (int i = 0; i < buttons.size(); i++) {
			int w = buttonWidth(ui, buttons.get(i));
			if (cx > x + PAD && cx + w > x + width - PAD) {
				cx = x + PAD;
				cy += BUTTON_HEIGHT + GAP;
			}
			if (over(mouseX, mouseY, cx, cy, w, BUTTON_HEIGHT)) {
				pressed[i] = 1;
				buttons.get(i).action().run();
				return true;
			}
			cx += w + GAP;
		}
		return false;
	}
}
