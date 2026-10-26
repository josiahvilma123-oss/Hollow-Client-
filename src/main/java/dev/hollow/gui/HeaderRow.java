package dev.hollow.gui;

import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.GuiGraphics;

/** A collapsible section header such as "CAMERA" or "ORES & MINERALS · DIAMOND ORE". */
final class HeaderRow extends Row {
	/** Collapse state survives closing and reopening the menu. */
	private static final Map<String, Boolean> COLLAPSED = new ConcurrentHashMap<>();
	final String key;
	final String title;
	private double arrow;

	HeaderRow(String moduleId, String title, boolean collapsedByDefault) {
		this.key = moduleId + "/" + title;
		this.title = title;
		COLLAPSED.putIfAbsent(key, collapsedByDefault);
		arrow = collapsed() ? 0 : 1;
	}

	boolean collapsed() {
		return COLLAPSED.getOrDefault(key, false);
	}

	@Override
	int height(UiContext ui) {
		return 20;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, ui);
		hover = ui.approach(hover, hovered ? 1 : 0, 0.06);
		arrow = ui.approach(arrow, collapsed() ? 0 : 1, 0.05);
		int lineY = y + 15;
		g.fill(x + PAD, lineY, x + width - PAD, lineY + 1, ui.c(Theme.BORDER_SOFT));
		int color = dev.hollow.core.util.Colors.lerp(Theme.accentDark(), ui.accentLight, (float) (0.55 + hover * 0.45));
		String label = title.toUpperCase(Locale.ROOT);
		Draw2D.spaced(g, ui.font, label, x + PAD + 10, y + 5, ui.c(color), 1);
		// Chevron: points right when collapsed, down when open.
		float cx = x + PAD + 3;
		float cy = y + 8.5f;
		float a = (float) (arrow * Math.PI / 2);
		float dx = (float) Math.cos(a);
		float dy = (float) Math.sin(a);
		Draw2D.line(g, cx - dx * 2 - dy * 2.5f, cy - dy * 2 + dx * 2.5f, cx + dx * 2, cy + dy * 2, 1.2f, ui.c(color));
		Draw2D.line(g, cx - dx * 2 + dy * 2.5f, cy - dy * 2 - dx * 2.5f, cx + dx * 2, cy + dy * 2, 1.2f, ui.c(color));
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		COLLAPSED.put(key, !collapsed());
		return true;
	}
}
