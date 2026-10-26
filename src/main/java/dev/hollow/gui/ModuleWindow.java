package dev.hollow.gui;

import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Transition;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

/** Floating settings window for one module, opened by right-clicking it in a category window. */
final class ModuleWindow {
	static final int TITLE = 24;
	private static final float RADIUS = 8f;

	final Module module;
	final List<Row> rows;
	final Transition open;
	float x;
	float y;
	float width;
	float height;
	private final Toggle toggle;
	private double scroll;
	private double scrollTarget;
	private double closeHover;
	private boolean closing;

	ModuleWindow(Module module, float x, float y, float width, float height, double openSeconds) {
		this.module = module;
		this.rows = buildRows(module);
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.toggle = new Toggle(module.isEnabled());
		this.open = new Transition(openSeconds, false);
		this.open.open();
	}

	void close(double seconds) {
		closing = true;
		open.duration(seconds);
		open.close();
	}

	boolean closing() {
		return closing;
	}

	boolean closed() {
		return closing && open.fullyClosed();
	}

	private static List<Row> buildRows(Module module) {
		List<Row> result = new ArrayList<>();
		PreviewRow preview = PreviewRow.forModule(module);
		if (preview != null) {
			result.add(preview);
		}
		String group = null;
		boolean manyGroups = module.settings().stream().map(Setting::group).distinct().count() > 8;
		for (Setting<?> setting : module.settings()) {
			if (!setting.group().equals(group)) {
				group = setting.group();
				if (!group.isEmpty()) {
					boolean collapse = manyGroups && group.contains("·");
					result.add(new HeaderRow(module.id(), group, collapse));
				}
			}
			Row row = rowFor(setting);
			if (row != null) {
				result.add(row);
			}
		}
		return result;
	}

	private static Row rowFor(Setting<?> setting) {
		if (setting instanceof BoolSetting bool) {
			return new BoolRow(bool);
		}
		if (setting instanceof NumberSetting number) {
			return new NumberRow(number);
		}
		if (setting instanceof ColorSetting color) {
			return new ColorRow(color);
		}
		if (setting instanceof EnumSetting<?> enumSetting) {
			return new EnumRow(enumSetting);
		}
		if (setting instanceof KeySetting key) {
			return new KeyRow(key);
		}
		if (setting instanceof StringListSetting list) {
			return new ListRow(list);
		}
		if (setting instanceof ActionSetting action) {
			return new ActionRow(action);
		}
		return null;
	}

	// ---------------------------------------------------------------- geometry

	boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	boolean overTitle(double mouseX, double mouseY) {
		return contains(mouseX, mouseY) && mouseY < y + TITLE;
	}

	private float closeX() {
		return x + width - 14;
	}

	private float closeY() {
		return y + TITLE / 2f;
	}

	boolean overClose(double mouseX, double mouseY) {
		double dx = mouseX - closeX();
		double dy = mouseY - closeY();
		return dx * dx + dy * dy <= 49;
	}

	private int toggleX() {
		return (int) (x + width - 30 - Toggle.WIDTH);
	}

	boolean overToggle(double mouseX, double mouseY) {
		return module.isToggleable() && mouseX >= toggleX() - 4 && mouseX < toggleX() + Toggle.WIDTH + 2
			&& mouseY >= y + 5 && mouseY < y + TITLE - 4;
	}

	private int descriptionHeight(UiContext ui) {
		return Math.min(2, descriptionLines(ui).size()) * 10 + 8;
	}

	private List<FormattedCharSequence> descriptionLines(UiContext ui) {
		String text = module.unavailableReason().map(r -> "Unavailable: " + r).orElse(module.description());
		return ui.font.split(FormattedText.of(text), (int) width - 20);
	}

	float bodyTop(UiContext ui) {
		return y + TITLE + descriptionHeight(ui);
	}

	float bodyBottom() {
		return y + height - 6;
	}

	boolean overBody(double mouseX, double mouseY, UiContext ui) {
		return contains(mouseX, mouseY) && mouseY >= bodyTop(ui) && mouseY < bodyBottom();
	}

	void scrollBy(double amount) {
		scrollTarget -= amount;
	}

	// ---------------------------------------------------------------- filtering

	List<Row> visibleRows(String query) {
		boolean searching = !query.isEmpty();
		String q = query.toLowerCase(Locale.ROOT);
		boolean moduleMatches = searching && module.name().toLowerCase(Locale.ROOT).contains(q);
		List<Row> result = new ArrayList<>();
		HeaderRow header = null;
		boolean headerAdded = false;
		for (Row row : rows) {
			if (row instanceof PreviewRow) {
				if (!searching) {
					result.add(row);
				}
				continue;
			}
			if (row instanceof HeaderRow h) {
				header = h;
				headerAdded = false;
				continue;
			}
			Setting<?> setting = row.setting();
			boolean matches = !searching || moduleMatches || setting.matches(q)
				|| header != null && header.title.toLowerCase(Locale.ROOT).contains(q);
			if (!setting.isVisible() || !matches) {
				continue;
			}
			if (header != null && !headerAdded) {
				result.add(header);
				headerAdded = true;
			}
			if (header == null || searching || !header.collapsed()) {
				result.add(row);
			}
		}
		return result;
	}

	Row focusedRow() {
		for (Row row : rows) {
			if (row.capturesKeyboard()) {
				return row;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- rendering

	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY, String query, double trail) {
		double e = open.eased(Easing.CUBIC_OUT);
		float savedAlpha = ui.alpha;
		ui.alpha = savedAlpha * (float) e;
		float scale = (float) (0.94 + 0.06 * e);
		float offset = (float) ((1 - e) * 10);

		// GUI motion blur: faint frame afterimages trailing the window as it settles.
		if (trail > 0 && e < 0.99) {
			for (int i = 2; i >= 1; i--) {
				float ghostAlpha = (float) (trail * 0.25 * (1 - e) / i);
				Draw2D.rounded(g, x, y + offset * (1 + i * 0.8f), width, height, RADIUS, Colors.fade(Theme.BACKGROUND, ghostAlpha));
			}
		}

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x + width / 2f, y + height / 2f + offset);
		pose.scale(scale, scale);
		pose.translate(-(x + width / 2f), -(y + height / 2f));

		Draw2D.rounded(g, x + 3, y + 4, width, height, RADIUS, ui.c(0x60000000));
		Draw2D.glow(g, x, y, width, height, RADIUS, ui.c(Colors.withAlpha(ui.accent, 150)), 7, 0.85f);
		Draw2D.rounded(g, x, y, width, height, RADIUS, ui.c(Colors.withAlpha(Theme.BACKGROUND, 245)));

		// Title bar.
		int titleColor = Colors.lerp(Theme.accentDark(), Theme.PANEL, 0.35f);
		Draw2D.rounded(g, x, y, width, TITLE, RADIUS, ui.c(titleColor));
		g.fill((int) x, (int) (y + TITLE - RADIUS), (int) (x + width), (int) (y + TITLE), ui.c(titleColor));
		g.fill((int) x + 1, (int) (y + TITLE), (int) (x + width) - 1, (int) (y + TITLE) + 1, ui.c(Colors.withAlpha(ui.accent, 120)));
		Draw2D.scythe(g, x + 8, y + 5, 13, ui.c(ui.accentLight));
		pose.pushMatrix();
		pose.translate(x + 26, y + 7);
		pose.scale(1.25f, 1.25f);
		Draw2D.text(g, ui.font, module.name(), 0, 0, ui.c(Theme.TEXT));
		pose.popMatrix();
		if (module.isToggleable()) {
			int tx = toggleX();
			String state = module.isEnabled() ? "ON" : "OFF";
			Draw2D.text(g, ui.font, state, tx - 5 - ui.font.width(state), (int) y + 8,
				ui.c(module.isEnabled() ? ui.accentLight : Theme.TEXT_FAINT));
			toggle.render(g, ui, tx, (int) y + 7, module.isEnabled(), 0);
		}
		boolean closeHovered = overClose(mouseX, mouseY);
		closeHover = ui.approach(closeHover, closeHovered ? 1 : 0, 0.05);
		if (closeHover > 0.01) {
			Draw2D.circle(g, closeX(), closeY(), 7, ui.c(Colors.withAlpha(Theme.DANGER, (int) (closeHover * 200))));
		}
		float cx = closeX();
		float cy = closeY();
		int cross = Colors.lerp(Theme.TEXT_DIM, 0xFFFFFFFF, (float) closeHover);
		Draw2D.line(g, cx - 3, cy - 3, cx + 3, cy + 3, 1.3f, ui.c(cross));
		Draw2D.line(g, cx - 3, cy + 3, cx + 3, cy - 3, 1.3f, ui.c(cross));

		// Description.
		int lineY = (int) (y + TITLE + 5);
		int descriptionColor = module.isAvailable() ? Theme.TEXT_DIM : Theme.DANGER;
		List<FormattedCharSequence> lines = descriptionLines(ui);
		for (int i = 0; i < Math.min(2, lines.size()); i++) {
			g.drawString(ui.font, lines.get(i), (int) x + 10, lineY, ui.c(descriptionColor), false);
			lineY += 10;
		}

		renderRows(g, ui, mouseX, mouseY, query);
		Draw2D.roundedBorder(g, x, y, width, height, RADIUS, 1, ui.c(Colors.withAlpha(Theme.BORDER, 230)));
		pose.popMatrix();
		ui.alpha = savedAlpha;
	}

	private void renderRows(GuiGraphics g, UiContext ui, int mouseX, int mouseY, String query) {
		List<Row> visible = visibleRows(query);
		float top = bodyTop(ui);
		float bottom = bodyBottom();
		int contentHeight = 0;
		for (Row row : visible) {
			row.ui = ui;
			row.width = (int) width - 14;
			contentHeight += row.height(ui) + 2;
		}
		double maxScroll = Math.max(0, contentHeight - (bottom - top) + 6);
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		scroll = ui.approach(scroll, scrollTarget, 0.05);
		g.enableScissor((int) x + 1, (int) top, (int) (x + width) - 1, (int) bottom);
		int rowY = (int) top + 2 - (int) Math.round(scroll);
		boolean insideBody = mouseY >= top && mouseY < bottom && contains(mouseX, mouseY);
		for (Row row : visible) {
			row.x = (int) x + 4;
			row.y = rowY;
			int h = row.height(ui);
			if (rowY + h >= top && rowY <= bottom) {
				row.render(g, ui, insideBody ? mouseX : -1000, insideBody ? mouseY : -1000);
			}
			rowY += h + 2;
		}
		if (visible.isEmpty()) {
			Draw2D.centered(g, ui.font, "No settings match \"" + query + "\"", (int) (x + width / 2), (int) top + 16, ui.c(Theme.TEXT_DIM));
		}
		g.fillGradient((int) x + 1, (int) top, (int) (x + width) - 1, (int) top + 6, ui.c(Theme.BACKGROUND), ui.c(Theme.BACKGROUND & 0x00FFFFFF));
		g.fillGradient((int) x + 1, (int) bottom - 6, (int) (x + width) - 1, (int) bottom, ui.c(Theme.BACKGROUND & 0x00FFFFFF), ui.c(Theme.BACKGROUND));
		g.disableScissor();
		if (maxScroll > 0) {
			float trackH = bottom - top;
			float barH = Math.max(16, trackH * trackH / (contentHeight + trackH));
			float barY = top + (float) ((trackH - barH) * (scroll / maxScroll));
			Draw2D.rounded(g, x + width - 5, barY, 2.5f, barH, 1.25f, ui.c(Colors.withAlpha(ui.accent, 160)));
		}
	}

	/** Finds the row under the cursor within the scrolled body. */
	Row rowAt(double mouseX, double mouseY, String query, UiContext ui) {
		if (!overBody(mouseX, mouseY, ui)) {
			return null;
		}
		for (Row row : visibleRows(query)) {
			if (row.contains(mouseX, mouseY, ui)) {
				return row;
			}
		}
		return null;
	}
}
