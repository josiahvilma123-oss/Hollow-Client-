package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.HollowClient;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Transition;
import dev.hollow.core.module.Category;
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
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * The Hollow module browser: categories and modules on the left, a searchable settings page
 * with a live preview on the right.
 */
public final class HollowScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 138;
	private static final int HEADER_HEIGHT = 30;
	private static final int PAGE_HEADER_HEIGHT = 44;
	private static final int ENTRY_HEIGHT = 18;
	private static String lastModuleId = "motion_blur";

	private final Screen parent;
	private final UiContext ui = new UiContext();
	private final TextInput search = new TextInput("Search settings…", 40);
	private final Transition open = new Transition(0.2, false);
	private final Transition page = new Transition(0.2, true);
	private final Map<Module, Toggle> moduleToggles = new HashMap<>();
	private final Map<Module, Double> entryHover = new HashMap<>();
	private Module selected;
	private Module previous;
	private List<Row> rows = List.of();
	private Row activeRow;
	private double scroll;
	private double scrollTarget;
	private double selectionY = -1;
	private long lastFrame = -1;
	private boolean closing;

	// Layout, recomputed every frame.
	private int winX;
	private int winY;
	private int winW;
	private int winH;

	public HollowScreen(Screen parent) {
		super(Component.literal("Hollow"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (selected == null) {
			Module initial = Hollow.modules().byId(lastModuleId).orElse(Hollow.modules().all().get(0));
			select(initial, false);
		}
		open.duration(guiSeconds());
		open.open();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private double guiSeconds() {
		return Hollow.module(MotionBlurModule.class).guiDuration.get() / 1000.0;
	}

	private void select(Module module, boolean animate) {
		if (module == selected) {
			return;
		}
		previous = selected;
		selected = module;
		lastModuleId = module.id();
		rows = buildRows(module);
		activeRow = null;
		scroll = 0;
		scrollTarget = 0;
		page.duration(guiSeconds());
		if (animate) {
			page.snap(false);
			page.open();
		} else {
			page.snap(true);
		}
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
					// Long pages (Block Finder categories) start with per-category sections collapsed.
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

	// ---------------------------------------------------------------- layout helpers

	private void layout() {
		winW = Math.min(width - 20, 600);
		winH = Math.min(height - 20, 360);
		winX = (width - winW) / 2;
		winY = (height - winH) / 2;
	}

	private int contentX() {
		return winX + SIDEBAR_WIDTH;
	}

	private int contentWidth() {
		return winW - SIDEBAR_WIDTH;
	}

	private int rowsTop() {
		return winY + HEADER_HEIGHT + PAGE_HEADER_HEIGHT;
	}

	private int rowsBottom() {
		return winY + winH - 6;
	}

	private int searchX() {
		return winX + winW - 8 - searchWidth();
	}

	private int searchWidth() {
		return Math.min(150, winW / 3);
	}

	private String query() {
		return search.text().trim();
	}

	/** Rows that pass the search filter, collapse state and visibility rules, in order. */
	private List<Row> visibleRows() {
		String query = query();
		boolean searching = !query.isEmpty();
		boolean moduleMatches = searching && selected.name().toLowerCase().contains(query.toLowerCase());
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
			boolean matches = !searching || moduleMatches || setting.matches(query) || header != null && header.title.toLowerCase().contains(query.toLowerCase());
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

	private int matchCount(Module module) {
		String query = query();
		if (query.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (Setting<?> setting : module.settings()) {
			if (setting.matches(query)) {
				count++;
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float dim = Hollow.module(InterfaceModule.class).backdrop.getFloat();
		float e = (float) open.eased(Easing.CUBIC_OUT);
		int top = Colors.withAlpha(0xFF050308, Math.round(255 * dim * e));
		int bottom = Colors.withAlpha(0xFF12081C, Math.round(255 * Math.min(1, dim * 1.15f) * e));
		g.fillGradient(0, 0, width, height, top, bottom);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		double delta = lastFrame < 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
		lastFrame = now;
		open.update(delta);
		page.update(delta);
		layout();
		InterfaceModule settings = Hollow.module(InterfaceModule.class);
		ui.font = font;
		ui.delta = delta;
		ui.animationSpeed = settings.animationSpeed.get();
		ui.descriptions = settings.descriptions.get();
		ui.accent = Theme.accent();
		ui.accentLight = Theme.accentLight();

		double e = open.eased(Easing.CUBIC_OUT);
		ui.alpha = (float) e;
		double trail = Hollow.module(MotionBlurModule.class).guiTrail();
		float offsetY = (float) ((1 - e) * 14);
		float scale = (float) (0.965 + 0.035 * e);

		Matrix3x2fStack pose = g.pose();
		// GUI motion blur: faint afterimages of the window frame trailing its entrance.
		if (trail > 0 && e < 0.995) {
			for (int i = 3; i >= 1; i--) {
				float ghostAlpha = (float) (trail * 0.22 * (1 - e) / i);
				int ghostY = Math.round(offsetY * (1 + i * 0.6f));
				Draw2D.panel(g, winX, winY + ghostY, winW, winH, Colors.fade(Theme.BACKGROUND, ghostAlpha));
				Draw2D.border(g, winX, winY + ghostY, winW, winH, Colors.fade(ui.accent, ghostAlpha));
			}
		}
		pose.pushMatrix();
		pose.translate(width / 2f, height / 2f + offsetY);
		pose.scale(scale, scale);
		pose.translate(-width / 2f, -height / 2f);

		float panelOpacity = settings.panelOpacity.getFloat();
		Draw2D.glow(g, winX, winY, winW, winH, ui.c(Colors.withAlpha(ui.accent, 150)), 8, 0.8f);
		Draw2D.panel(g, winX, winY, winW, winH, ui.c(Colors.fade(Theme.BACKGROUND, panelOpacity)));
		Draw2D.border(g, winX, winY, winW, winH, ui.c(Theme.BORDER));

		renderHeader(g, mouseX, mouseY);
		renderSidebar(g, mouseX, mouseY);
		renderPage(g, mouseX, mouseY, trail);
		pose.popMatrix();
	}

	private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
		g.fill(winX + 1, winY + HEADER_HEIGHT - 1, winX + winW - 1, winY + HEADER_HEIGHT, ui.c(Theme.BORDER));
		Draw2D.scythe(g, winX + 10, winY + 7, 16, ui.c(ui.accentLight));
		int textX = winX + 32;
		int wordmark = Draw2D.spaced(g, font, "HOLLOW", textX, winY + 11, ui.c(Theme.TEXT), 3);
		Draw2D.text(g, font, "·  MODULE BROWSER", textX + wordmark + 8, winY + 11, ui.c(Theme.TEXT_FAINT));
		search.render(g, ui, searchX(), winY + 8, searchWidth(), 15);
		if (search.text().isEmpty() && !search.focused()) {
			Draw2D.text(g, font, "⌕", searchX() + searchWidth() - 11, winY + 11, ui.c(Theme.TEXT_FAINT));
		}
	}

	private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
		int sx = winX + 1;
		int top = winY + HEADER_HEIGHT;
		g.fill(sx, top, winX + SIDEBAR_WIDTH, winY + winH - 1, ui.c(Colors.withAlpha(Theme.PANEL, 200)));
		g.fill(winX + SIDEBAR_WIDTH - 1, top, winX + SIDEBAR_WIDTH, winY + winH - 1, ui.c(Theme.BORDER));
		int y = top + 8;
		String query = query();
		for (Category category : Category.values()) {
			List<Module> modules = Hollow.modules().byCategory(category);
			if (modules.isEmpty()) {
				continue;
			}
			Draw2D.spaced(g, font, category.displayName().toUpperCase(), sx + 10, y + 2, ui.c(Theme.TEXT_FAINT), 1);
			y += 14;
			for (Module module : modules) {
				boolean isSelected = module == selected;
				if (isSelected) {
					selectionY = selectionY < 0 ? y : ui.approach(selectionY, y, 0.05);
					int sy = (int) Math.round(selectionY);
					Draw2D.panel(g, sx + 5, sy, SIDEBAR_WIDTH - 12, ENTRY_HEIGHT - 2, ui.c(Theme.CARD_HOVER));
					g.fill(sx + 5, sy + 3, sx + 7, sy + ENTRY_HEIGHT - 5, ui.c(ui.accent));
					Draw2D.glow(g, sx + 5, sy + 3, 2, ENTRY_HEIGHT - 8, ui.c(ui.accent), 3, 0.9f);
				}
				boolean hovered = mouseX >= sx + 5 && mouseX < sx + SIDEBAR_WIDTH - 7 && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 2;
				double h = ui.approach(entryHover.getOrDefault(module, 0.0), hovered ? 1 : 0, 0.06);
				entryHover.put(module, h);
				if (!isSelected && h > 0.02) {
					Draw2D.panel(g, sx + 5, y, SIDEBAR_WIDTH - 12, ENTRY_HEIGHT - 2, ui.c(Colors.withAlpha(Theme.CARD, (int) (h * 255))));
				}
				int matches = matchCount(module);
				boolean dimmed = !query.isEmpty() && matches == 0 && !module.matches(query);
				int nameColor = dimmed ? Theme.TEXT_FAINT : isSelected ? Theme.TEXT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, (float) h);
				Draw2D.text(g, font, Draw2D.ellipsize(font, module.name(), SIDEBAR_WIDTH - 44), sx + 13, y + 4, ui.c(nameColor));
				int dot = !module.isAvailable() ? Theme.DANGER : module.isEnabled() ? ui.accent : Theme.OFF;
				int dotX = sx + SIDEBAR_WIDTH - 18;
				if (module.isEnabled()) {
					Draw2D.glow(g, dotX, y + 5, 4, 4, ui.c(Colors.withAlpha(ui.accent, 170)), 2, 1f);
				}
				Draw2D.panel(g, dotX, y + 5, 4, 4, ui.c(dot));
				if (matches > 0) {
					String badge = Integer.toString(matches);
					Draw2D.text(g, font, badge, dotX - 4 - font.width(badge), y + 4, ui.c(ui.accentLight));
				}
				y += ENTRY_HEIGHT;
			}
			y += 6;
		}
		String version = FabricLoader.getInstance().getModContainer(Hollow.MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		String key = HollowClient.openMenuKey() == null ? "" : " · " + HollowClient.openMenuKey().getTranslatedKeyMessage().getString();
		Draw2D.text(g, font, Draw2D.ellipsize(font, "v" + version + key, SIDEBAR_WIDTH - 16), sx + 10, winY + winH - 14, ui.c(Theme.TEXT_FAINT));
	}

	private void renderPage(GuiGraphics g, int mouseX, int mouseY, double trail) {
		double pe = page.eased(Easing.CUBIC_OUT);
		float savedAlpha = ui.alpha;
		int cx = contentX();
		int cw = contentWidth();
		int headerTop = winY + HEADER_HEIGHT;

		// Previous page title slides out with a short trail while the new one slides in.
		if (previous != null && pe < 1) {
			ui.alpha = savedAlpha * (float) (1 - pe);
			int slideOut = (int) Math.round(-18 * pe);
			drawPageTitle(g, previous, cx + 14 + slideOut, headerTop + 8, mouseX, mouseY, false);
			if (trail > 0) {
				ui.alpha = savedAlpha * (float) ((1 - pe) * trail * 0.35);
				drawPageTitle(g, previous, cx + 14 + slideOut + 6, headerTop + 8, mouseX, mouseY, false);
			}
		}
		ui.alpha = savedAlpha * (float) pe;
		int slideIn = (int) Math.round(16 * (1 - pe));
		if (trail > 0 && pe < 0.98) {
			float base = ui.alpha;
			ui.alpha = base * (float) (trail * 0.3);
			drawPageTitle(g, selected, cx + 14 + slideIn * 2, headerTop + 8, mouseX, mouseY, false);
			ui.alpha = base;
		}
		drawPageTitle(g, selected, cx + 14 + slideIn, headerTop + 8, mouseX, mouseY, true);
		g.fill(cx + 10, rowsTop() - 4, cx + cw - 10, rowsTop() - 3, ui.c(Theme.BORDER_SOFT));

		// Rows.
		List<Row> visible = visibleRows();
		int viewportTop = rowsTop();
		int viewportBottom = rowsBottom();
		int contentHeight = 0;
		for (Row row : visible) {
			row.ui = ui;
			contentHeight += row.height(ui) + 2;
		}
		double maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop) + 8);
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		scroll = ui.approach(scroll, scrollTarget, 0.05);
		g.enableScissor(cx + 1, viewportTop, cx + cw - 1, viewportBottom);
		int y = viewportTop + 2 - (int) Math.round(scroll);
		for (Row row : visible) {
			row.x = cx + 6 + slideIn;
			row.y = y;
			row.width = cw - 18;
			int h = row.height(ui);
			if (y + h >= viewportTop && y <= viewportBottom) {
				boolean inside = mouseY >= viewportTop && mouseY < viewportBottom;
				row.render(g, ui, inside ? mouseX : -1000, inside ? mouseY : -1000);
			}
			y += h + 2;
		}
		if (visible.isEmpty()) {
			Draw2D.centered(g, font, "No settings match \"" + query() + "\"", cx + cw / 2, viewportTop + 20, ui.c(Theme.TEXT_DIM));
		}
		// Soft fade at the viewport edges.
		g.fillGradient(cx + 1, viewportTop, cx + cw - 1, viewportTop + 6, ui.c(Theme.BACKGROUND), ui.c(Theme.BACKGROUND & 0x00FFFFFF));
		g.fillGradient(cx + 1, viewportBottom - 6, cx + cw - 1, viewportBottom, ui.c(Theme.BACKGROUND & 0x00FFFFFF), ui.c(Theme.BACKGROUND));
		g.disableScissor();
		if (maxScroll > 0) {
			int trackH = viewportBottom - viewportTop;
			int barH = Math.max(16, (int) (trackH * trackH / (double) (contentHeight + trackH)));
			int barY = viewportTop + (int) ((trackH - barH) * (scroll / maxScroll));
			g.fill(cx + cw - 5, barY, cx + cw - 3, barY + barH, ui.c(Colors.withAlpha(ui.accent, 150)));
		}
		ui.alpha = savedAlpha;
	}

	private void drawPageTitle(GuiGraphics g, Module module, int x, int y, int mouseX, int mouseY, boolean interactive) {
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(1.5f, 1.5f);
		Draw2D.text(g, font, module.name(), 0, 0, ui.c(Theme.TEXT));
		pose.popMatrix();
		int cw = contentWidth();
		String subtitle = module.unavailableReason().map(r -> "Unavailable: " + r).orElse(module.description());
		int subtitleColor = module.isAvailable() ? Theme.TEXT_DIM : Theme.DANGER;
		Draw2D.text(g, font, Draw2D.ellipsize(font, subtitle, cw - 80), x, y + 18, ui.c(subtitleColor));
		if (module.isToggleable() && interactive) {
			Toggle toggle = moduleToggles.computeIfAbsent(module, m -> new Toggle(m.isEnabled()));
			int tx = contentX() + cw - 14 - Toggle.WIDTH;
			String state = module.isEnabled() ? "ON" : "OFF";
			Draw2D.text(g, font, state, tx - 6 - font.width(state), y + 4, ui.c(module.isEnabled() ? ui.accentLight : Theme.TEXT_FAINT));
			toggle.render(g, ui, tx, y + 3, module.isEnabled(), 0);
		}
	}

	// ---------------------------------------------------------------- input

	private Row focusedRow() {
		for (Row row : rows) {
			if (row.capturesKeyboard()) {
				return row;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) {
			return true;
		}
		layout();
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		Row focused = focusedRow();
		if (focused != null && !focused.contains(mx, my, ui)) {
			focused.cancelInput();
		}
		boolean onSearch = mx >= searchX() && mx < searchX() + searchWidth() && my >= winY + 8 && my < winY + 23;
		search.focus(onSearch);
		if (onSearch) {
			if (button == 1) {
				search.setText("");
			}
			return true;
		}
		if (clickSidebar(mx, my, button)) {
			return true;
		}
		int cw = contentWidth();
		int tx = contentX() + cw - 14 - Toggle.WIDTH;
		int headerTop = winY + HEADER_HEIGHT + 8;
		if (selected.isToggleable() && mx >= tx - 30 && mx < tx + Toggle.WIDTH + 2 && my >= headerTop && my < headerTop + 16) {
			selected.toggle();
			return true;
		}
		if (my >= rowsTop() && my < rowsBottom()) {
			for (Row row : visibleRows()) {
				if (row.contains(mx, my, ui)) {
					activeRow = row;
					return row.mouseClicked(mx, my, button);
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean clickSidebar(double mx, double my, int button) {
		if (mx < winX || mx >= winX + SIDEBAR_WIDTH) {
			return false;
		}
		int y = winY + HEADER_HEIGHT + 8;
		for (Category category : Category.values()) {
			List<Module> modules = Hollow.modules().byCategory(category);
			if (modules.isEmpty()) {
				continue;
			}
			y += 14;
			for (Module module : modules) {
				if (my >= y && my < y + ENTRY_HEIGHT - 2) {
					if (button == 1 && module.isToggleable()) {
						module.toggle();
					} else {
						select(module, true);
					}
					return true;
				}
				y += ENTRY_HEIGHT;
			}
			y += 6;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (activeRow != null && activeRow.mouseDragged(event.x(), event.y())) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (activeRow != null) {
			activeRow.mouseReleased();
			activeRow = null;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= contentX() && mouseX < contentX() + contentWidth() && mouseY >= rowsTop() && mouseY < rowsBottom()) {
			if (minecraft.hasShiftDown()) {
				for (Row row : visibleRows()) {
					if (row instanceof NumberRow number && row.contains(mouseX, mouseY, ui)) {
						number.nudge(scrollY > 0 ? 1 : -1);
						return true;
					}
				}
			}
			scrollTarget -= scrollY * 28;
			return true;
		}
		if (mouseX >= winX && mouseX < winX + SIDEBAR_WIDTH) {
			List<Module> all = Hollow.modules().all();
			int index = all.indexOf(selected) + (scrollY > 0 ? -1 : 1);
			if (index >= 0 && index < all.size()) {
				select(all.get(index), true);
			}
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
			return focused.keyPressed(event) || true;
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
			return search.keyPressed(event) || true;
		}
		if (event.key() == GLFW.GLFW_KEY_F && event.hasControlDown()) {
			search.focus(true);
			return true;
		}
		if (event.isEscape() || HollowClient.openMenuKey() != null && HollowClient.openMenuKey().matches(event)) {
			onClose();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
			List<Module> all = Hollow.modules().all();
			int index = all.indexOf(selected) + (event.key() == GLFW.GLFW_KEY_UP ? -1 : 1);
			if (index >= 0 && index < all.size()) {
				select(all.get(index), true);
			}
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
			scrollTarget = 0;
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
		lastFrame = -1;
	}

	@Override
	public void removed() {
		if (Hollow.config() != null) {
			Hollow.config().save();
		}
	}

	/** Exposed for tests and for other screens that want to deep-link into a module page. */
	public HollowScreen openOn(Class<? extends Module> type) {
		select(Hollow.module(type), false);
		return this;
	}

}
