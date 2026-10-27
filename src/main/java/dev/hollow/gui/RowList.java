package dev.hollow.gui;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A scrolling, searchable column of rows with collapsible section headers and keyboard focus.
 * Used by module settings, the keybind page and the profile page.
 */
final class RowList {
	private static final int GAP = 2;

	private List<Row> rows;
	/** Extra text that makes every row match, e.g. the module name. */
	private final String title;
	private double scroll;
	private double scrollTarget;
	private Row keyboardRow;
	private final List<Row> visible = new ArrayList<>();
	private String visibleFor;
	int x;
	int y;
	int width;
	int height;

	RowList(List<Row> rows, String title) {
		this.rows = rows;
		this.title = title.toLowerCase(Locale.ROOT);
	}

	void replace(List<Row> newRows) {
		rows = newRows;
		visibleFor = null;
		keyboardRow = null;
	}

	List<Row> rows() {
		return rows;
	}

	/** Rows shown for a search query: headers only appear when one of their rows matches. */
	List<Row> visibleRows(String query) {
		List<Row> result = new ArrayList<>();
		boolean searching = !query.isEmpty();
		String q = query.toLowerCase(Locale.ROOT);
		boolean allMatch = searching && !title.isEmpty() && title.contains(q);
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
			boolean matches = !searching || allMatch || row.matches(q) || header != null && header.title.toLowerCase(Locale.ROOT).contains(q);
			if (!row.visible() || !matches) {
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

	void scrollBy(double amount) {
		scrollTarget -= amount;
	}

	void scrollToTop() {
		scroll = scrollTarget = 0;
	}

	boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY, String query, int background) {
		visible.clear();
		visible.addAll(visibleRows(query));
		visibleFor = query;
		int contentHeight = 0;
		for (Row row : visible) {
			row.ui = ui;
			row.width = width - 8;
			contentHeight += row.height(ui) + GAP;
		}
		double maxScroll = Math.max(0, contentHeight - height + 6);
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		scroll = ui.approach(scroll, scrollTarget, 0.05);
		g.enableScissor(x, y, x + width, y + height);
		int rowY = y + 2 - (int) Math.round(scroll);
		boolean inside = contains(mouseX, mouseY);
		for (Row row : visible) {
			row.x = x;
			row.y = rowY;
			row.keyboardFocus = row == keyboardRow;
			int h = row.height(ui);
			if (rowY + h >= y && rowY <= y + height) {
				row.render(g, ui, inside ? mouseX : -1000, inside ? mouseY : -1000);
				if (row == keyboardRow) {
					Draw2D.roundedBorder(g, row.x + 1, rowY, row.width - 2, h, 4, 1, ui.c(Colors.withAlpha(ui.detail, 220)));
				}
			}
			rowY += h + GAP;
		}
		if (visible.isEmpty()) {
			String text = query.isEmpty() ? "Nothing here yet" : "No settings match \"" + query + "\"";
			Draw2D.centered(g, ui.font, Draw2D.ellipsize(ui.font, text, width - 16), x + width / 2, y + 16, ui.c(Theme.TEXT_DIM));
		}
		int clear = background & 0x00FFFFFF;
		g.fillGradient(x, y, x + width, y + 5, ui.c(background), ui.c(clear));
		g.fillGradient(x, y + height - 5, x + width, y + height, ui.c(clear), ui.c(background));
		g.disableScissor();
		if (maxScroll > 0) {
			float barH = Math.max(16, (float) height * height / (contentHeight + height));
			float barY = y + (float) ((height - barH) * (scroll / maxScroll));
			Draw2D.rounded(g, x + width - 3, barY, 2.5f, barH, 1.25f, ui.c(Colors.withAlpha(ui.accent, 160)));
		}
	}

	/** The row under the cursor, using the layout of the last frame. */
	Row rowAt(double mouseX, double mouseY, UiContext ui) {
		if (!contains(mouseX, mouseY)) {
			return null;
		}
		for (Row row : visible) {
			if (row.contains(mouseX, mouseY, ui)) {
				return row;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- keyboard

	boolean hasKeyboardFocus() {
		return keyboardRow != null && visible.contains(keyboardRow);
	}

	void clearKeyboardFocus() {
		keyboardRow = null;
	}

	/** Moves keyboard focus to the next or previous focusable row and scrolls it into view. */
	void moveFocus(int direction, UiContext ui) {
		List<Row> focusable = visible.stream().filter(Row::focusable).toList();
		if (focusable.isEmpty()) {
			keyboardRow = null;
			return;
		}
		int index = focusable.indexOf(keyboardRow);
		if (index < 0) {
			index = direction > 0 ? 0 : focusable.size() - 1;
		} else {
			index = Math.max(0, Math.min(focusable.size() - 1, index + direction));
		}
		keyboardRow = focusable.get(index);
		ensureVisible(keyboardRow, ui);
	}

	private void ensureVisible(Row row, UiContext ui) {
		int offset = 2;
		for (Row r : visible) {
			if (r == row) {
				break;
			}
			offset += r.height(ui) + GAP;
		}
		int h = row.height(ui);
		if (offset < scrollTarget + 4) {
			scrollTarget = Math.max(0, offset - 4);
		} else if (offset + h > scrollTarget + height - 4) {
			scrollTarget = offset + h - height + 4;
		}
	}

	Row keyboardRow() {
		return hasKeyboardFocus() ? keyboardRow : null;
	}
}
