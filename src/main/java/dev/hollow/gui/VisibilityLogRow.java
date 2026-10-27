package dev.hollow.gui;

import dev.hollow.exploration.VisibilityLog;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** The Entity Visibility event log, newest first. */
final class VisibilityLogRow extends Row {
	private static final int MAX_SHOWN = 30;

	@Override
	boolean focusable() {
		return false;
	}

	@Override
	boolean matches(String query) {
		return "event log".contains(query) || VisibilityLog.INSTANCE.events().stream().anyMatch(e -> e.text().toLowerCase().contains(query));
	}

	@Override
	int height(UiContext ui) {
		return 6 + Math.max(1, Math.min(MAX_SHOWN, VisibilityLog.INSTANCE.events().size())) * 11;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		List<VisibilityLog.Event> events = VisibilityLog.INSTANCE.events();
		Draw2D.rounded(g, x + PAD, y, width - PAD * 2, height(ui) - 2, 4, ui.c(0xFF0A080D));
		if (events.isEmpty()) {
			Draw2D.text(g, ui.font, "No events yet.", x + PAD + 6, y + 4, ui.c(Theme.TEXT_FAINT));
			return;
		}
		int ly = y + 4;
		for (int i = events.size() - 1, shown = 0; i >= 0 && shown < MAX_SHOWN; i--, shown++) {
			VisibilityLog.Event event = events.get(i);
			Draw2D.text(g, ui.font, event.time(), x + PAD + 6, ly, ui.c(Theme.TEXT_FAINT));
			int tx = x + PAD + 10 + ui.font.width("00:00:00");
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, event.text(), x + width - PAD - 6 - tx), tx, ly, ui.c(Theme.TEXT_DIM));
			ly += 11;
		}
	}
}
