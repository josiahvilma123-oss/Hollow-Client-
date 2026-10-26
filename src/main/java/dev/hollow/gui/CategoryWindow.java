package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A draggable, collapsible window listing one category's modules, Meteor-style:
 * left-click toggles a module, right-click opens its settings.
 */
final class CategoryWindow {
	static final int WIDTH = 132;
	static final int HEADER = 20;
	static final int ENTRY = 17;
	private static final int PAD = 4;
	private static final float RADIUS = 6f;

	final Category category;
	final List<Module> modules;
	float x;
	float y;
	boolean collapsed;
	private double expand;
	private double headerHover;
	private final Map<Module, Double> hover = new HashMap<>();
	private final Map<Module, Double> active = new HashMap<>();

	CategoryWindow(Category category, float x, float y, boolean collapsed) {
		this.category = category;
		this.modules = Hollow.modules().byCategory(category);
		this.x = x;
		this.y = y;
		this.collapsed = collapsed;
		this.expand = collapsed ? 0 : 1;
	}

	String id() {
		return category.name().toLowerCase(Locale.ROOT);
	}

	private List<Module> visibleModules(String query) {
		if (query.isEmpty()) {
			return modules;
		}
		return modules.stream().filter(m -> m.matches(query)).toList();
	}

	float bodyHeight(String query) {
		List<Module> visible = visibleModules(query);
		return visible.size() * ENTRY + PAD * 2 + (visible.isEmpty() ? 12 : 0);
	}

	float height(String query) {
		return HEADER + (float) (bodyHeight(query) * expand);
	}

	boolean contains(double mouseX, double mouseY, String query) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + height(query);
	}

	boolean overHeader(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEADER;
	}

	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY, String query, Module selected, boolean focused) {
		expand = ui.approach(expand, collapsed ? 0 : 1, 0.05);
		float h = height(query);
		float panelOpacity = ui.alpha;

		// Shadow, glow and body.
		Draw2D.rounded(g, x + 2, y + 3, WIDTH, h, RADIUS, ui.c(0x50000000));
		if (focused) {
			Draw2D.glow(g, x, y, WIDTH, h, RADIUS, ui.c(Colors.withAlpha(ui.accent, 120)), 5, 0.8f);
		}
		Draw2D.rounded(g, x, y, WIDTH, h, RADIUS, ui.c(Colors.withAlpha(Theme.PANEL, 236)));

		// Header: accent gradient strip with the category name, Meteor-style centered title.
		boolean overHeader = overHeader(mouseX, mouseY);
		headerHover = ui.approach(headerHover, overHeader ? 1 : 0, 0.06);
		int headerTop = Colors.lerp(Theme.accentDark(), ui.accent, (float) (0.35 + headerHover * 0.25));
		Draw2D.rounded(g, x, y, WIDTH, HEADER, RADIUS, ui.c(headerTop));
		if (expand > 0.02) {
			// Square off the header's lower corners where it meets the body.
			g.fill((int) x, (int) (y + HEADER - RADIUS), (int) (x + WIDTH), (int) (y + HEADER), ui.c(headerTop));
		}
		String title = category.displayName();
		Draw2D.centered(g, ui.font, title, (int) (x + WIDTH / 2f), (int) y + 6, ui.c(Theme.TEXT));
		drawChevron(g, ui, x + WIDTH - 11, y + HEADER / 2f);
		Draw2D.roundedBorder(g, x, y, WIDTH, h, RADIUS, 1, ui.c(Colors.withAlpha(Theme.BORDER, 220)));

		if (expand < 0.02) {
			return;
		}
		g.enableScissor((int) x, (int) (y + HEADER), (int) (x + WIDTH), (int) Math.ceil(y + h));
		float rowY = y + HEADER + PAD;
		List<Module> visible = visibleModules(query);
		if (visible.isEmpty()) {
			Draw2D.centered(g, ui.font, "No matches", (int) (x + WIDTH / 2f), (int) rowY + 2, ui.c(Theme.TEXT_FAINT));
		}
		for (Module module : visible) {
			renderEntry(g, ui, module, rowY, mouseX, mouseY, query, module == selected);
			rowY += ENTRY;
		}
		g.disableScissor();
	}

	private void renderEntry(GuiGraphics g, UiContext ui, Module module, float rowY, int mouseX, int mouseY, String query, boolean selected) {
		float ex = x + PAD;
		float ew = WIDTH - PAD * 2;
		boolean hovered = mouseX >= ex && mouseX < ex + ew && mouseY >= rowY && mouseY < rowY + ENTRY - 1;
		double h = ui.approach(hover.getOrDefault(module, 0.0), hovered ? 1 : 0, 0.05);
		hover.put(module, h);
		boolean on = module.isToggleable() && module.isEnabled();
		double a = ui.approach(active.getOrDefault(module, on ? 1.0 : 0.0), on ? 1 : 0, 0.06);
		active.put(module, a);

		if (a > 0.01) {
			int fill = Colors.withAlpha(ui.accent, (int) (a * (70 + h * 40)));
			Draw2D.rounded(g, ex, rowY, ew, ENTRY - 1, 4, ui.c(fill));
			Draw2D.rounded(g, ex + 2, rowY + 4, 2, ENTRY - 9, 1, ui.c(Colors.withAlpha(ui.accentLight, (int) (a * 255))));
		} else if (h > 0.01) {
			Draw2D.rounded(g, ex, rowY, ew, ENTRY - 1, 4, ui.c(Colors.withAlpha(Theme.CARD_HOVER, (int) (h * 255))));
		}
		if (selected) {
			Draw2D.roundedBorder(g, ex, rowY, ew, ENTRY - 1, 4, 1, ui.c(Colors.withAlpha(ui.accentLight, 200)));
		}
		int textColor;
		if (!module.isAvailable()) {
			textColor = Theme.DANGER;
		} else if (on) {
			textColor = Theme.TEXT;
		} else {
			textColor = Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, (float) h);
		}
		String name = Draw2D.ellipsize(ui.font, module.name(), (int) ew - 22);
		Draw2D.text(g, ui.font, name, (int) ex + 8, (int) rowY + 4, ui.c(textColor));

		// Right side: search match count, or a settings hint on hover.
		int matches = matchCount(module, query);
		if (matches > 0) {
			String badge = Integer.toString(matches);
			int bw = ui.font.width(badge) + 6;
			Draw2D.rounded(g, ex + ew - bw - 3, rowY + 3, bw, 10, 5, ui.c(Colors.withAlpha(ui.accent, 150)));
			Draw2D.text(g, ui.font, badge, (int) (ex + ew - bw), (int) rowY + 4, ui.c(Theme.TEXT));
		} else if (h > 0.3) {
			Draw2D.text(g, ui.font, "›", (int) (ex + ew - 9), (int) rowY + 4, ui.c(Colors.withAlpha(Theme.TEXT_FAINT, (int) (h * 255))));
		}
	}

	private static int matchCount(Module module, String query) {
		if (query.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (var setting : module.settings()) {
			if (setting.isVisible() && setting.matches(query)) {
				count++;
			}
		}
		return count;
	}

	private void drawChevron(GuiGraphics g, UiContext ui, float cx, float cy) {
		float a = (float) (expand * Math.PI / 2);
		float dx = (float) Math.cos(a);
		float dy = (float) Math.sin(a);
		int color = ui.c(Colors.withAlpha(Theme.TEXT, 200));
		Draw2D.line(g, cx - dx * 2 - dy * 2.5f, cy - dy * 2 + dx * 2.5f, cx + dx * 2, cy + dy * 2, 1.2f, color);
		Draw2D.line(g, cx - dx * 2 + dy * 2.5f, cy - dy * 2 - dx * 2.5f, cx + dx * 2, cy + dy * 2, 1.2f, color);
	}

	/** The module under the cursor, or null. */
	Module moduleAt(double mouseX, double mouseY, String query) {
		if (expand < 0.5 || mouseX < x + PAD || mouseX >= x + WIDTH - PAD) {
			return null;
		}
		float rowY = y + HEADER + PAD;
		for (Module module : visibleModules(query)) {
			if (mouseY >= rowY && mouseY < rowY + ENTRY - 1) {
				return module;
			}
			rowY += ENTRY;
		}
		return null;
	}

	/** Description of the hovered module, for the tooltip. */
	Module hovered(double mouseX, double mouseY, String query) {
		return moduleAt(mouseX, mouseY, query);
	}
}
