package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.HollowClient;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Transition;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * Hollow's click GUI: draggable, collapsible category windows (left-click a module to toggle
 * it, right-click for its settings), a floating settings window, and a search bar.
 */
public final class HollowScreen extends Screen {
	private static final String SETTINGS_WINDOW_ID = "settings";
	private static final int BAR_HEIGHT = 22;
	private static final long TOOLTIP_DELAY_MS = 350;

	private final Screen parent;
	private final UiContext ui = new UiContext();
	private final TextInput search = new TextInput("Search modules & settings…", 40);
	private final Transition open = new Transition(0.2, false);
	private final List<CategoryWindow> windows = new ArrayList<>();
	private final List<ModuleWindow> closingWindows = new ArrayList<>();
	private ModuleWindow moduleWindow;
	private Module pendingOpen;
	private Row activeRow;
	private Object dragTarget;
	private double dragOffsetX;
	private double dragOffsetY;
	private double dragStartX;
	private double dragStartY;
	private boolean dragMoved;
	private Module tooltipModule;
	private long tooltipSince;
	private long lastFrame = -1;
	private boolean closing;

	public HollowScreen(Screen parent) {
		super(Component.literal("Hollow"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		InterfaceModule settings = Hollow.module(InterfaceModule.class);
		if (windows.isEmpty()) {
			// Default layout: categories stacked in a left column, leaving room for settings.
			float nextY = BAR_HEIGHT + 24;
			for (Category category : Category.values()) {
				if (Hollow.modules().byCategory(category).isEmpty()) {
					continue;
				}
				String id = category.name().toLowerCase(Locale.ROOT);
				int[] saved = settings.windowPosition(id);
				CategoryWindow window = new CategoryWindow(category, saved != null ? saved[0] : 16, saved != null ? saved[1] : nextY,
					saved != null && saved[2] == 1);
				windows.add(window);
				nextY += window.height("") + 12;
			}
		}
		for (CategoryWindow window : windows) {
			window.x = clamp(window.x, 0, width - CategoryWindow.WIDTH);
			window.y = clamp(window.y, 0, height - CategoryWindow.HEADER);
		}
		if (moduleWindow != null) {
			moduleWindow.x = clamp(moduleWindow.x, 0, width - moduleWindow.width);
			moduleWindow.y = clamp(moduleWindow.y, 0, height - ModuleWindow.TITLE);
		}
		open.duration(guiSeconds());
		open.open();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(Math.max(min, max), value));
	}

	private double guiSeconds() {
		return Hollow.module(MotionBlurModule.class).guiDuration.get() / 1000.0;
	}

	private String query() {
		return search.text().trim();
	}

	/** Opens (or switches) the settings window to a module. */
	void openSettings(Module module) {
		if (moduleWindow != null && moduleWindow.module == module) {
			return;
		}
		float besideColumn = 16 + CategoryWindow.WIDTH + 16;
		float roomBeside = width - besideColumn - 8;
		// Prefer sitting beside the category column; shrink to fit there on small screens.
		float w = roomBeside >= 260 ? Math.min(380, roomBeside) : Math.min(380, width - 32);
		float h = Math.min(height - BAR_HEIGHT - 32, 360);
		float x;
		float y;
		if (moduleWindow != null) {
			x = moduleWindow.x;
			y = moduleWindow.y;
			moduleWindow.close(guiSeconds() * 0.6);
			closingWindows.add(moduleWindow);
		} else {
			int[] saved = Hollow.module(InterfaceModule.class).windowPosition(SETTINGS_WINDOW_ID);
			x = saved != null ? saved[0] : besideColumn + w <= width - 8 ? besideColumn : (width - w) / 2f;
			y = saved != null ? saved[1] : BAR_HEIGHT + 24;
		}
		x = clamp(x, 0, width - w);
		y = clamp(y, 0, height - h);
		moduleWindow = new ModuleWindow(module, x, y, w, h, guiSeconds());
		activeRow = null;
	}

	private void closeSettings() {
		if (moduleWindow != null) {
			moduleWindow.close(guiSeconds() * 0.7);
			closingWindows.add(moduleWindow);
			moduleWindow = null;
			activeRow = null;
		}
	}

	/** Opens the menu with a module's settings already showing (used by tests and deep links). */
	public HollowScreen openOn(Class<? extends Module> type) {
		pendingOpen = Hollow.module(type);
		return this;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float dim = Hollow.module(InterfaceModule.class).backdrop.getFloat();
		float e = (float) open.eased(Easing.CUBIC_OUT);
		int top = Colors.withAlpha(0xFF050308, Math.round(255 * dim * 0.85f * e));
		int bottom = Colors.withAlpha(0xFF14081F, Math.round(255 * Math.min(1, dim * 1.1f) * e));
		g.fillGradient(0, 0, width, height, top, bottom);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (pendingOpen != null) {
			openSettings(pendingOpen);
			pendingOpen = null;
		}
		long now = System.nanoTime();
		double delta = lastFrame < 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
		lastFrame = now;
		open.update(delta);
		InterfaceModule settings = Hollow.module(InterfaceModule.class);
		ui.font = font;
		ui.delta = delta;
		ui.animationSpeed = settings.animationSpeed.get();
		ui.descriptions = settings.descriptions.get();
		ui.accent = Theme.accent();
		ui.accentLight = Theme.accentLight();
		double e = open.eased(Easing.CUBIC_OUT);
		double trail = Hollow.module(MotionBlurModule.class).guiTrail();
		String query = query();
		// While a settings window is open, search filters its settings instead of the module list.
		String windowQuery = moduleWindow != null ? "" : query;

		renderTopBar(g, (float) e);

		// Category windows fade and drop in one after another.
		boolean blocked = moduleWindow != null && moduleWindow.contains(mouseX, mouseY);
		for (int i = 0; i < windows.size(); i++) {
			CategoryWindow window = windows.get(i);
			double stagger = Math.max(0, Math.min(1, e * 1.4 - i * 0.12));
			ui.alpha = (float) stagger;
			float drop = (float) ((1 - stagger) * 10);
			if (trail > 0 && stagger < 0.98) {
				Draw2D.rounded(g, window.x, window.y - drop * 2, CategoryWindow.WIDTH, window.height(windowQuery), 6,
					Colors.fade(Theme.PANEL, (float) (trail * 0.3 * (1 - stagger))));
			}
			window.y -= drop;
			window.render(g, ui, blocked ? -1000 : mouseX, blocked ? -1000 : mouseY, windowQuery,
				moduleWindow == null ? null : moduleWindow.module, i == windows.size() - 1);
			window.y += drop;
		}
		ui.alpha = (float) e;

		closingWindows.removeIf(ModuleWindow::closed);
		for (ModuleWindow closingWindow : closingWindows) {
			closingWindow.open.update(delta);
			closingWindow.render(g, ui, -1000, -1000, "", trail);
		}
		if (moduleWindow != null) {
			moduleWindow.open.update(delta);
			moduleWindow.render(g, ui, mouseX, mouseY, query, trail);
		}
		renderTooltip(g, mouseX, mouseY, windowQuery);
		renderHint(g);
		ui.alpha = 1f;
	}

	private float barWidth() {
		return Math.min(width - 20, 300);
	}

	private float barX() {
		return (width - barWidth()) / 2f;
	}

	private int searchX() {
		return (int) (barX() + 92);
	}

	private int searchWidth() {
		return (int) (barWidth() - 98);
	}

	private void renderTopBar(GuiGraphics g, float alpha) {
		ui.alpha = alpha;
		float bx = barX();
		float by = 8 - (1 - alpha) * 8;
		float bw = barWidth();
		Draw2D.glow(g, bx, by, bw, BAR_HEIGHT, BAR_HEIGHT / 2f, ui.c(Colors.withAlpha(ui.accent, 110)), 5, 0.8f);
		Draw2D.rounded(g, bx, by, bw, BAR_HEIGHT, BAR_HEIGHT / 2f, ui.c(Colors.withAlpha(Theme.PANEL, 240)));
		Draw2D.roundedBorder(g, bx, by, bw, BAR_HEIGHT, BAR_HEIGHT / 2f, 1, ui.c(Colors.withAlpha(Theme.BORDER, 230)));
		Draw2D.scythe(g, bx + 9, by + 4, 14, ui.c(ui.accentLight));
		Draw2D.spaced(g, font, "HOLLOW", (int) bx + 28, (int) by + 7, ui.c(Theme.TEXT), 2);
		search.render(g, ui, searchX(), (int) by + 4, searchWidth(), 14);
	}

	private void renderTooltip(GuiGraphics g, int mouseX, int mouseY, String windowQuery) {
		Module hovered = null;
		if (dragTarget == null && (moduleWindow == null || !moduleWindow.contains(mouseX, mouseY))) {
			for (int i = windows.size() - 1; i >= 0; i--) {
				CategoryWindow window = windows.get(i);
				if (window.contains(mouseX, mouseY, windowQuery)) {
					hovered = window.moduleAt(mouseX, mouseY, windowQuery);
					break;
				}
			}
		}
		long now = System.currentTimeMillis();
		if (hovered != tooltipModule) {
			tooltipModule = hovered;
			tooltipSince = now;
		}
		if (hovered == null || now - tooltipSince < TOOLTIP_DELAY_MS) {
			return;
		}
		float fade = Math.min(1f, (now - tooltipSince - TOOLTIP_DELAY_MS) / 150f);
		List<FormattedCharSequence> lines = new ArrayList<>(font.split(FormattedText.of(hovered.description()), 170));
		String hint = hovered.isToggleable() ? "Left-click: toggle · Right-click: settings" : "Click: settings";
		int w = font.width(hint);
		for (FormattedCharSequence line : lines) {
			w = Math.max(w, font.width(line));
		}
		w += 12;
		int h = lines.size() * 10 + 18;
		int tx = Math.min(mouseX + 10, width - w - 4);
		int ty = Math.min(mouseY + 10, height - h - 4);
		float saved = ui.alpha;
		ui.alpha = fade;
		Draw2D.rounded(g, tx, ty, w, h, 5, ui.c(0xF00B0910));
		Draw2D.roundedBorder(g, tx, ty, w, h, 5, 1, ui.c(Colors.withAlpha(ui.accent, 160)));
		int ly = ty + 5;
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, tx + 6, ly, ui.c(Theme.TEXT), false);
			ly += 10;
		}
		Draw2D.text(g, font, hint, tx + 6, ly + 2, ui.c(Theme.TEXT_FAINT));
		ui.alpha = saved;
	}

	private void renderHint(GuiGraphics g) {
		String key = HollowClient.openMenuKey() == null ? "Right Shift" : HollowClient.openMenuKey().getTranslatedKeyMessage().getString();
		String hint = "Click: toggle · Right-click: settings · Drag headers · " + key + ": close";
		// Keep clear of the hotbar in the bottom centre.
		int maxWidth = Math.max(60, width / 2 - 100);
		Draw2D.text(g, font, Draw2D.ellipsize(font, hint, maxWidth), 8, height - 14, ui.c(Colors.withAlpha(Theme.TEXT_FAINT, 200)));
	}

	// ---------------------------------------------------------------- input

	private Row focusedRow() {
		return moduleWindow == null ? null : moduleWindow.focusedRow();
	}

	private void bringToFront(CategoryWindow window) {
		windows.remove(window);
		windows.add(window);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) {
			return true;
		}
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		String query = query();

		Row focused = focusedRow();
		if (focused != null && !focused.contains(mx, my, ui)) {
			focused.cancelInput();
		}
		boolean onSearch = mx >= searchX() && mx < searchX() + searchWidth() && my >= 12 && my < 26;
		search.focus(onSearch);
		if (onSearch) {
			if (button == 1) {
				search.setText("");
			}
			return true;
		}

		if (moduleWindow != null && moduleWindow.contains(mx, my)) {
			if (moduleWindow.overClose(mx, my)) {
				closeSettings();
				return true;
			}
			if (moduleWindow.overToggle(mx, my)) {
				moduleWindow.module.toggle();
				return true;
			}
			if (moduleWindow.overTitle(mx, my)) {
				startDrag(moduleWindow, mx, my, moduleWindow.x, moduleWindow.y);
				return true;
			}
			Row row = moduleWindow.rowAt(mx, my, query, ui);
			if (row != null) {
				activeRow = row;
				return row.mouseClicked(mx, my, button);
			}
			return true;
		}

		String windowQuery = moduleWindow != null ? "" : query;
		for (int i = windows.size() - 1; i >= 0; i--) {
			CategoryWindow window = windows.get(i);
			if (!window.contains(mx, my, windowQuery)) {
				continue;
			}
			bringToFront(window);
			if (window.overHeader(mx, my)) {
				if (button == 1) {
					toggleCollapse(window);
				} else {
					startDrag(window, mx, my, window.x, window.y);
				}
				return true;
			}
			Module module = window.moduleAt(mx, my, windowQuery);
			if (module != null) {
				if (button == 0 && module.isToggleable()) {
					module.toggle();
				} else if (button == 0 || button == 1) {
					openSettings(module);
				}
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void toggleCollapse(CategoryWindow window) {
		window.collapsed = !window.collapsed;
		Hollow.module(InterfaceModule.class).saveWindowPosition(window.id(), Math.round(window.x), Math.round(window.y), window.collapsed);
	}

	private void startDrag(Object target, double mx, double my, float x, float y) {
		dragTarget = target;
		dragOffsetX = mx - x;
		dragOffsetY = my - y;
		dragStartX = mx;
		dragStartY = my;
		dragMoved = false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mx = event.x();
		double my = event.y();
		if (dragTarget != null) {
			if (Math.abs(mx - dragStartX) + Math.abs(my - dragStartY) > 2) {
				dragMoved = true;
			}
			float nx = (float) (mx - dragOffsetX);
			float ny = (float) (my - dragOffsetY);
			if (dragTarget instanceof CategoryWindow window) {
				window.x = clamp(nx, 0, width - CategoryWindow.WIDTH);
				window.y = clamp(ny, 0, height - CategoryWindow.HEADER);
			} else if (dragTarget instanceof ModuleWindow window) {
				window.x = clamp(nx, 0, width - window.width);
				window.y = clamp(ny, 0, height - ModuleWindow.TITLE);
			}
			return true;
		}
		if (activeRow != null && activeRow.mouseDragged(mx, my)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragTarget != null) {
			InterfaceModule settings = Hollow.module(InterfaceModule.class);
			if (dragTarget instanceof CategoryWindow window) {
				if (dragMoved) {
					settings.saveWindowPosition(window.id(), Math.round(window.x), Math.round(window.y), window.collapsed);
				} else {
					toggleCollapse(window);
				}
			} else if (dragTarget instanceof ModuleWindow window && dragMoved) {
				settings.saveWindowPosition(SETTINGS_WINDOW_ID, Math.round(window.x), Math.round(window.y), false);
			}
			dragTarget = null;
			return true;
		}
		if (activeRow != null) {
			activeRow.mouseReleased();
			activeRow = null;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (moduleWindow != null && moduleWindow.overBody(mouseX, mouseY, ui)) {
			if (minecraft.hasShiftDown()) {
				Row row = moduleWindow.rowAt(mouseX, mouseY, query(), ui);
				if (row instanceof NumberRow number) {
					number.nudge(scrollY > 0 ? 1 : -1);
					return true;
				}
			}
			moduleWindow.scrollBy(scrollY * 28);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		Row focused = focusedRow();
		if (focused != null) {
			if (event.isEscape() && !(focused instanceof KeyRow)) {
				focused.cancelInput();
				return true;
			}
			focused.keyPressed(event);
			return true;
		}
		if (search.focused()) {
			if (event.isEscape()) {
				if (search.text().isEmpty()) {
					search.focus(false);
				} else {
					search.setText("");
				}
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				search.focus(false);
				return true;
			}
			search.keyPressed(event);
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_F && event.hasControlDown()) {
			search.focus(true);
			return true;
		}
		if (event.isEscape()) {
			if (moduleWindow != null) {
				closeSettings();
			} else {
				onClose();
			}
			return true;
		}
		if (HollowClient.openMenuKey() != null && HollowClient.openMenuKey().matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		Row focused = focusedRow();
		if (focused != null) {
			return focused.charTyped(event);
		}
		if (!search.focused() && event.isAllowedChatCharacter() && !Character.isWhitespace(event.codepoint())) {
			search.focus(true);
		}
		if (search.charTyped(event)) {
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public void tick() {
		if (closing && open.fullyClosed()) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public void onClose() {
		if (closing) {
			return;
		}
		closing = true;
		open.duration(guiSeconds() * 0.8);
		open.close();
		closeSettings();
	}

	@Override
	public void removed() {
		if (Hollow.config() != null) {
			Hollow.config().save();
		}
	}
}
