package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.HollowClient;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Transition;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.util.Colors;
import dev.hollow.hud.HudEditorScreen;
import dev.hollow.input.KeyInput;
import dev.hollow.menu.MenuScene;
import dev.hollow.menu.TitleScreenSwap;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * The Holo click GUI: one central window with a header (search, profile, HUD editor), a
 * collapsible category sidebar, a grid of module cards and an expandable settings panel, plus
 * keybind and profile pages. Fully usable with mouse or keyboard; saves on close.
 */
public final class HoloScreen extends Screen {
	private static final int HEADER = 28;
	private static final int FOOTER = 16;
	private static final int SIDEBAR = 118;
	private static final int SIDEBAR_COMPACT = 30;
	private static final int MIN_CARD = 150;
	private static final int GAP = 8;
	private static final long TOOLTIP_DELAY_MS = 450;
	private static final long MESSAGE_MS = 4500;

	private enum Page {
		MODULES,
		KEYBINDS,
		PROFILES,
		HUD_EDITOR
	}

	private record Nav(String label, Icons.Icon icon, Category category, Page page, String tip) {
	}

	private record Wrapped(int width, List<String> lines, boolean truncated) {
	}

	private final Screen parent;
	private final UiContext ui = new UiContext();
	private final TextInput search = new TextInput("Search modules & settings  (Ctrl+F)", 40);
	private final Transition open = new Transition(0.2, false);
	private final Transition pageIn = new Transition(0.18, true);
	private final Transition panelIn = new Transition(0.2, false);
	private final List<Nav> navs = new ArrayList<>();
	private int toolsStart;
	private Page page = Page.MODULES;
	private Category category;
	private SettingsPanel panel;
	private KeybindsPage keybinds;
	private ProfilesPage profiles;
	private Module pendingOpen;
	private String pendingNavLabel;
	private Row activeRow;
	private boolean closing;
	private long lastFrame = -1;

	// Layout of the current frame.
	private int winX;
	private int winY;
	private int winW;
	private int winH;
	private int sideW;
	private int contentX;
	private int contentY;
	private int contentW;
	private int contentH;
	private int searchX;
	private int searchW;
	private int closeX;
	private int hudX;
	private int hudW;
	private int profileX;
	private int profileW;

	// Sidebar.
	private final Map<Nav, Double> navHover = new HashMap<>();
	private double sideScroll;
	private int navItemH = 17;

	// Card grid.
	private final List<Module> cards = new ArrayList<>();
	private final Map<Module, Toggle> toggles = new HashMap<>();
	private final Map<Module, double[]> cardAnim = new HashMap<>();
	private final Map<Module, Wrapped> wrapped = new HashMap<>();
	private int columns = 1;
	private int cardW;
	private int cardH;
	private int gridTop;
	private double gridScroll;
	private double gridScrollTarget;
	private int cardFocus = -1;
	private boolean keyboardNav;

	// Footer and tooltip.
	private String message = "";
	private long messageAt;
	private String tooltip;
	private String shownTooltip;
	private long tooltipSince;

	public HoloScreen(Screen parent) {
		super(Component.literal("Holo"));
		this.parent = parent;
	}

	/** Opens the menu with a module's settings already showing (main-menu shortcuts and tests). */
	public HoloScreen openOn(Class<? extends Module> type) {
		pendingOpen = Hollow.module(type);
		return this;
	}

	/** Opens the menu on a category page. */
	public HoloScreen openCategory(Category target) {
		pendingNavLabel = target.displayName();
		return this;
	}

	/** Opens the menu on the Keybinds or Profiles page. */
	public HoloScreen openKeybinds() {
		pendingNavLabel = "Keybinds";
		return this;
	}

	public HoloScreen openProfiles() {
		pendingNavLabel = "Profiles";
		return this;
	}

	/** The module whose settings panel is open, or null. */
	public Module openModule() {
		return panel != null && panelIn.isOpening() ? panel.module : null;
	}

	private static InterfaceModule settings() {
		return Hollow.module(InterfaceModule.class);
	}

	private boolean reducedMotion() {
		return settings().reducedMotion.get();
	}

	private double seconds(double factor) {
		return reducedMotion() ? 1e-4 : Hollow.module(MotionBlurModule.class).guiDuration.get() / 1000.0 * factor;
	}

	@Override
	protected void init() {
		if (navs.isEmpty()) {
			navs.add(new Nav("All Modules", Icons.Icon.ALL, null, Page.MODULES, "Every module, searchable"));
			for (Category c : Category.values()) {
				if (!Hollow.modules().byCategory(c).isEmpty()) {
					navs.add(new Nav(c.displayName(), Icons.of(c), c, Page.MODULES, c.description()));
				}
			}
			toolsStart = navs.size();
			navs.add(new Nav("Keybinds", Icons.Icon.KEYBINDS, null, Page.KEYBINDS, "Every Holo keybind, with conflict warnings"));
			navs.add(new Nav("Profiles", Icons.Icon.PROFILES, null, Page.PROFILES, "Save, load, share and reset whole setups"));
			navs.add(new Nav("HUD Editor", Icons.Icon.HUD_EDITOR, null, Page.HUD_EDITOR, "Drag HUD elements into place"));
		}
		if (pendingNavLabel != null) {
			navs.stream().filter(n -> n.label().equals(pendingNavLabel)).findFirst().ifPresent(this::select);
			pageIn.snap(true);
			pendingNavLabel = null;
		}
		if (pendingOpen != null) {
			category = pendingOpen.category();
			openSettings(pendingOpen);
			panelIn.snap(true);
			pendingOpen = null;
		}
		if (profiles != null) {
			profiles.refresh();
		}
		open.duration(seconds(1));
		if (reducedMotion()) {
			open.snap(true);
		}
		open.open();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private String query() {
		return search.text().trim();
	}

	void notify(String text) {
		message = text;
		messageAt = System.currentTimeMillis();
	}

	// ---------------------------------------------------------------- navigation

	private void select(Nav nav) {
		if (nav.page() == Page.HUD_EDITOR) {
			HudEditorScreen.open();
			return;
		}
		boolean changed = nav.page() != page || nav.category() != category;
		page = nav.page();
		category = nav.category();
		panel = null;
		panelIn.snap(false);
		activeRow = null;
		cardFocus = keyboardNav ? 0 : -1;
		if (page == Page.KEYBINDS && keybinds == null) {
			keybinds = new KeybindsPage(this::notify);
		}
		if (page == Page.PROFILES) {
			if (profiles == null) {
				profiles = new ProfilesPage(this::notify);
			} else {
				profiles.refresh();
			}
		}
		if (changed) {
			gridScroll = gridScrollTarget = 0;
			pageIn.duration(seconds(0.9));
			pageIn.snap(false);
			pageIn.open();
			if (reducedMotion()) {
				pageIn.snap(true);
			}
		}
	}

	private boolean selected(Nav nav) {
		return nav.page() == page && (page != Page.MODULES || nav.category() == category);
	}

	private void openSettings(Module module) {
		page = Page.MODULES;
		panel = new SettingsPanel(module);
		activeRow = null;
		panelIn.duration(seconds(1));
		panelIn.snap(false);
		panelIn.open();
		if (reducedMotion()) {
			panelIn.snap(true);
		}
	}

	private void closeSettings() {
		if (panel != null) {
			Row focused = panel.list.focusedRow();
			if (focused != null) {
				focused.cancelInput();
			}
			panelIn.duration(seconds(0.8));
			panelIn.close();
			if (reducedMotion()) {
				panelIn.snap(false);
			}
			activeRow = null;
		}
	}

	/** The row list that receives input right now, or null on the module grid. */
	private RowList activeList() {
		if (panel != null && panelIn.isOpening()) {
			return panel.list;
		}
		return switch (page) {
			case KEYBINDS -> keybinds == null ? null : keybinds.list;
			case PROFILES -> profiles == null ? null : profiles.list;
			default -> null;
		};
	}

	// ---------------------------------------------------------------- layout

	private boolean compactSidebar() {
		return settings().sidebarCollapsed.get() || winW < 440;
	}

	private void layout() {
		winW = Math.min(width - 16, 780);
		winH = Math.min(height - 16, 460);
		winX = (width - winW) / 2;
		winY = (height - winH) / 2;
		sideW = compactSidebar() ? SIDEBAR_COMPACT : SIDEBAR;
		contentX = winX + sideW + 10;
		contentY = winY + HEADER + 8;
		contentW = winW - sideW - 20;
		contentH = winH - HEADER - FOOTER - 12;

		closeX = winX + winW - 14;
		hudW = winW < 380 ? 22 : font.width("HUD") + 26;
		hudX = closeX - 12 - hudW;
		String profile = profileLabel();
		profileW = winW < 460 ? 22 : Math.min(120, font.width(profile) + 24);
		profileX = hudX - 6 - profileW;
		int wordmark = winW < 340 ? 44 : 92;
		searchX = winX + wordmark;
		searchW = Math.max(40, Math.min(280, profileX - 8 - searchX));
	}

	private String profileLabel() {
		String active = Hollow.config().activeProfile();
		return active.isEmpty() ? "No profile" : active;
	}

	private boolean overSearch(double mx, double my) {
		return Pill.over(mx, my, searchX, winY + 7, searchW, 15);
	}

	private boolean overClose(double mx, double my) {
		double dx = mx - closeX;
		double dy = my - (winY + HEADER / 2.0);
		return dx * dx + dy * dy <= 64;
	}

	private boolean overHud(double mx, double my) {
		return Pill.over(mx, my, hudX, winY + 7, hudW, Pill.HEIGHT);
	}

	private boolean overProfile(double mx, double my) {
		return Pill.over(mx, my, profileX, winY + 7, profileW, Pill.HEIGHT);
	}

	private boolean overSidebar(double mx, double my) {
		return Pill.over(mx, my, winX, winY + HEADER, sideW, winH - HEADER - FOOTER);
	}

	private boolean overSidebarToggle(double mx, double my) {
		return Pill.over(mx, my, winX + 4, winY + 6, 18, 16);
	}

	private int navTop() {
		return winY + HEADER + 6;
	}

	private int navY(int index) {
		int separator = index >= toolsStart ? 9 : 0;
		return navTop() + index * navItemH + separator - (int) sideScroll;
	}

	private Nav navAt(double mx, double my) {
		if (!overSidebar(mx, my) || my < navTop()) {
			return null;
		}
		for (int i = 0; i < navs.size(); i++) {
			int y = navY(i);
			if (my >= y && my < y + navItemH - 1) {
				return navs.get(i);
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) {
			if (TitleScreenSwap.sceneEnabled()) {
				MenuScene.render(g, width, height, -1, -1, -1);
			} else {
				renderPanorama(g, partialTick);
			}
		}
		float dim = settings().backdrop.getFloat();
		float e = (float) open.eased(Easing.CUBIC_OUT);
		int top = Colors.withAlpha(0xFF050308, Math.round(255 * dim * 0.85f * e));
		int bottom = Colors.withAlpha(0xFF14081F, Math.round(255 * Math.min(1, dim * 1.1f) * e));
		g.fillGradient(0, 0, width, height, top, bottom);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		double delta = lastFrame < 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
		lastFrame = now;
		open.update(delta);
		pageIn.update(delta);
		panelIn.update(delta);
		if (panel != null && panelIn.fullyClosed()) {
			panel = null;
		}
		InterfaceModule settings = settings();
		ui.font = font;
		ui.delta = delta;
		ui.animationSpeed = settings.animationSpeed.get();
		ui.descriptions = settings.descriptions.get();
		ui.reducedMotion = settings.reducedMotion.get();
		ui.accent = Theme.accent();
		ui.accentLight = Theme.accentLight();
		ui.detail = Theme.detail();
		tooltip = null;
		layout();

		float e = (float) open.eased(Easing.CUBIC_OUT);
		ui.alpha = e;
		var pose = g.pose();
		pose.pushMatrix();
		float scale = 0.96f + 0.04f * e;
		pose.translate(width / 2f, height / 2f + (1 - e) * 8);
		pose.scale(scale, scale);
		pose.translate(-width / 2f, -height / 2f);

		renderWindow(g);
		renderHeader(g, mouseX, mouseY);
		renderSidebar(g, mouseX, mouseY);
		renderContent(g, mouseX, mouseY);
		renderFooter(g);
		pose.popMatrix();
		renderTooltip(g, mouseX, mouseY);
		ui.alpha = 1f;
	}

	private void renderWindow(GuiGraphics g) {
		Draw2D.rounded(g, winX + 3, winY + 5, winW, winH, 10, ui.c(0x70000000));
		Draw2D.roundedBorder(g, winX - 1, winY - 1, winW + 2, winH + 2, 11, 1, ui.c(Colors.withAlpha(ui.accent, 70)));
		Draw2D.rounded(g, winX, winY, winW, winH, 10, ui.c(Colors.withAlpha(Theme.BACKGROUND, 248)));
		// Header band with a faint accent wash.
		int band = Colors.lerp(Theme.PANEL, Theme.accentDark(), 0.35f);
		Draw2D.rounded(g, winX, winY, winW, HEADER, 10, ui.c(band));
		g.fill(winX, winY + HEADER - 10, winX + winW, winY + HEADER, ui.c(band));
		g.fill(winX + 1, winY + HEADER, winX + winW - 1, winY + HEADER + 1, ui.c(Colors.withAlpha(ui.detail, 110)));
		// Sidebar panel.
		g.fill(winX + 1, winY + HEADER + 1, winX + sideW, winY + winH - FOOTER, ui.c(Colors.withAlpha(Theme.PANEL, 235)));
		g.fill(winX + sideW, winY + HEADER + 1, winX + sideW + 1, winY + winH - FOOTER, ui.c(Theme.BORDER_SOFT));
		// Footer.
		g.fill(winX + 1, winY + winH - FOOTER, winX + winW - 1, winY + winH - FOOTER + 1, ui.c(Theme.BORDER_SOFT));
		Draw2D.roundedBorder(g, winX, winY, winW, winH, 10, 1, ui.c(Colors.withAlpha(Theme.BORDER, 240)));
		// Gold corner ornaments.
		int gold = ui.c(Colors.withAlpha(ui.detail, 170));
		int arm = 9;
		for (int corner = 0; corner < 4; corner++) {
			boolean right = corner % 2 == 1;
			boolean bottom = corner >= 2;
			float cx = right ? winX + winW - 4 : winX + 4;
			float cy = bottom ? winY + winH - 4 : winY + 4;
			float sx = right ? -1 : 1;
			float sy = bottom ? -1 : 1;
			Draw2D.line(g, cx + sx * 3, cy, cx + sx * arm, cy, 1f, gold);
			Draw2D.line(g, cx, cy + sy * 3, cx, cy + sy * arm, 1f, gold);
		}
	}

	private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
		boolean toggleHovered = overSidebarToggle(mouseX, mouseY);
		if (toggleHovered) {
			Draw2D.rounded(g, winX + 4, winY + 6, 18, 16, 4, ui.c(Theme.CARD_HOVER));
			tooltip = winW < 440 ? "The window is too narrow for the full sidebar" : compactSidebar() ? "Expand sidebar" : "Collapse sidebar";
		}
		Icons.draw(g, Icons.Icon.MENU, winX + 13, winY + 14, 4f, ui.c(toggleHovered ? ui.detail : Theme.TEXT_DIM));
		Draw2D.scythe(g, winX + 26, winY + 7, 14, ui.c(ui.accentLight));
		if (winW >= 340) {
			Draw2D.spaced(g, font, "HOLO", winX + 44, winY + 10, ui.c(ui.detail), 2);
		}
		search.render(g, ui, searchX, winY + 7, searchW, 15);

		boolean profileHovered = overProfile(mouseX, mouseY);
		boolean profileSelected = page == Page.PROFILES && panel == null;
		drawHeaderButton(g, profileX, profileW, Icons.Icon.PROFILES, winW < 460 ? null : profileLabel(), profileHovered, profileSelected);
		if (profileHovered) {
			tooltip = "Profile: " + profileLabel() + ". Click to save, load or share profiles.";
		}
		boolean hudHovered = overHud(mouseX, mouseY);
		drawHeaderButton(g, hudX, hudW, Icons.Icon.HUD_EDITOR, winW < 380 ? null : "HUD", hudHovered, false);
		if (hudHovered) {
			tooltip = "Open the HUD editor: drag HUD elements, snap to guides, right-click to reset one.";
		}
		boolean closeHovered = overClose(mouseX, mouseY);
		float cy = winY + HEADER / 2f;
		if (closeHovered) {
			Draw2D.circle(g, closeX, cy, 7, ui.c(Colors.withAlpha(Theme.DANGER, 190)));
			tooltip = "Close (Esc). Settings are saved.";
		}
		int cross = ui.c(closeHovered ? 0xFFFFFFFF : Theme.TEXT_DIM);
		Draw2D.line(g, closeX - 3, cy - 3, closeX + 3, cy + 3, 1.3f, cross);
		Draw2D.line(g, closeX - 3, cy + 3, closeX + 3, cy - 3, 1.3f, cross);
	}

	private void drawHeaderButton(GuiGraphics g, int x, int w, Icons.Icon icon, String label, boolean hovered, boolean selected) {
		int y = winY + 7;
		int fill = Colors.lerp(Theme.CARD, ui.accentDarkOr(), hovered ? 0.6f : selected ? 0.45f : 0.15f);
		Draw2D.rounded(g, x, y, w, Pill.HEIGHT, Pill.HEIGHT / 2f, ui.c(fill));
		Draw2D.roundedBorder(g, x, y, w, Pill.HEIGHT, Pill.HEIGHT / 2f, 1, ui.c(selected ? ui.detail : hovered ? ui.accent : Theme.BORDER));
		int iconColor = ui.c(selected || hovered ? ui.detail : Theme.TEXT_DIM);
		if (label == null) {
			Icons.draw(g, icon, x + w / 2f, y + Pill.HEIGHT / 2f, 4f, iconColor);
		} else {
			Icons.draw(g, icon, x + 10, y + Pill.HEIGHT / 2f, 4f, iconColor);
			Draw2D.text(g, font, Draw2D.ellipsize(font, label, w - 24), x + 18, y + 3, ui.c(Theme.TEXT));
		}
	}

	private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
		boolean compact = compactSidebar();
		int top = winY + HEADER;
		int bottom = winY + winH - FOOTER;
		int available = bottom - navTop() - 4;
		int needed = navs.size() * 17 + 9;
		navItemH = needed <= available ? 17 : Math.max(12, (available - 9) / navs.size());
		int total = navs.size() * navItemH + 9;
		sideScroll = Math.max(0, Math.min(Math.max(0, total - available), sideScroll));

		g.enableScissor(winX, navTop() - 2, winX + sideW, bottom - 2);
		Nav hovered = navAt(mouseX, mouseY);
		for (int i = 0; i < navs.size(); i++) {
			Nav nav = navs.get(i);
			int y = navY(i);
			if (i == toolsStart) {
				g.fill(winX + 8, y - 5, winX + sideW - 8, y - 4, ui.c(Theme.BORDER));
			}
			boolean isSelected = selected(nav);
			double h = ui.approach(navHover.getOrDefault(nav, 0.0), nav == hovered ? 1 : 0, 0.05);
			navHover.put(nav, h);
			int itemX = winX + 4;
			int itemW = sideW - 8;
			if (isSelected) {
				Draw2D.rounded(g, itemX, y, itemW, navItemH - 1, 4, ui.c(Colors.withAlpha(ui.accent, 70)));
				Draw2D.rounded(g, itemX, y + 3, 2, navItemH - 7, 1, ui.c(ui.detail));
			} else if (h > 0.01) {
				Draw2D.rounded(g, itemX, y, itemW, navItemH - 1, 4, ui.c(Colors.withAlpha(Theme.CARD_HOVER, (int) (h * 255))));
			}
			int iconColor = isSelected ? ui.detail : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, (float) h);
			Icons.draw(g, nav.icon(), winX + 15, y + (navItemH - 1) / 2f, navItemH >= 15 ? 4.5f : 3.5f, ui.c(iconColor));
			if (!compact) {
				int textColor = isSelected ? Theme.TEXT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, (float) h);
				Draw2D.text(g, font, Draw2D.ellipsize(font, nav.label(), sideW - 52), winX + 26, y + (navItemH - 9) / 2, ui.c(textColor));
				if (nav.category() != null) {
					int on = enabledCount(nav.category());
					if (on > 0) {
						String count = Integer.toString(on);
						int cw = font.width(count) + 6;
						Draw2D.rounded(g, winX + sideW - 8 - cw, y + (navItemH - 10) / 2f, cw, 10, 5, ui.c(Colors.withAlpha(ui.accent, 110)));
						Draw2D.text(g, font, count, winX + sideW - 5 - cw, y + (navItemH - 9) / 2 + 1, ui.c(Theme.TEXT));
					}
				}
			}
			if (nav == hovered) {
				String counts = nav.category() == null ? "" : " (" + enabledCount(nav.category()) + " of "
					+ Hollow.modules().byCategory(nav.category()).size() + " on)";
				tooltip = compact ? nav.label() + counts + ": " + nav.tip() : nav.tip() + counts;
			}
		}
		g.disableScissor();
	}

	private static int enabledCount(Category category) {
		int count = 0;
		for (Module module : Hollow.modules().byCategory(category)) {
			if (module.isToggleable() && module.isEnabled()) {
				count++;
			}
		}
		return count;
	}

	private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
		boolean inside = Pill.over(mouseX, mouseY, contentX - 4, contentY - 4, contentW + 8, contentH + 8);
		int mx = inside ? mouseX : -1000;
		int my = inside ? mouseY : -1000;
		float pageE = (float) pageIn.eased(Easing.CUBIC_OUT);
		float saved = ui.alpha;
		ui.alpha = saved * pageE;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(0, (1 - pageE) * 6);
		String query = query();
		switch (page) {
			case KEYBINDS -> renderListPage(g, "Keybinds", "Every Holo keybind in one place. Conflicts are flagged.", keybinds.list, mx, my, query);
			case PROFILES -> renderListPage(g, "Profiles", "Active: " + profileLabel(), profiles.list, mx, my, query);
			default -> renderModules(g, mx, my, query);
		}
		pose.popMatrix();
		ui.alpha = saved;
	}

	private void renderTitle(GuiGraphics g, String title, String subtitle) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(contentX, contentY);
		pose.scale(1.4f, 1.4f);
		Draw2D.text(g, font, title, 0, 0, ui.c(Theme.TEXT));
		pose.popMatrix();
		int tw = (int) (font.width(title) * 1.4f);
		Draw2D.line(g, contentX + tw + 6, contentY + 6, contentX + tw + 9, contentY + 3, 1f, ui.c(ui.detail));
		Draw2D.line(g, contentX + tw + 9, contentY + 3, contentX + tw + 12, contentY + 6, 1f, ui.c(ui.detail));
		Draw2D.line(g, contentX + tw + 6, contentY + 6, contentX + tw + 9, contentY + 9, 1f, ui.c(ui.detail));
		Draw2D.line(g, contentX + tw + 9, contentY + 9, contentX + tw + 12, contentY + 6, 1f, ui.c(ui.detail));
		Draw2D.text(g, font, Draw2D.ellipsize(font, subtitle, contentW), contentX, contentY + 15, ui.c(Theme.TEXT_FAINT));
	}

	private void renderListPage(GuiGraphics g, String title, String subtitle, RowList list, int mx, int my, String query) {
		renderTitle(g, title, subtitle);
		list.x = contentX - 4;
		list.y = contentY + 28;
		list.width = contentW + 8;
		list.height = contentH - 28;
		list.render(g, ui, mx, my, query, Theme.BACKGROUND);
		Row row = list.rowAt(mx, my, ui);
		if (row != null && !ui.descriptions && row.setting() != null && !row.setting().description().isEmpty()) {
			tooltip = row.setting().description();
		}
	}

	private void renderModules(GuiGraphics g, int mx, int my, String query) {
		float panelE = panel == null ? 0 : (float) panelIn.eased(Easing.CUBIC_OUT);
		float saved = ui.alpha;
		if (panelE < 0.999f) {
			ui.alpha = saved * (1 - panelE);
			renderGrid(g, panelE > 0 ? -1000 : mx, panelE > 0 ? -1000 : my, query);
		}
		if (panel != null) {
			// GUI motion blur: faint afterimages trailing the panel while it slides.
			double trail = Hollow.module(MotionBlurModule.class).guiTrail();
			if (trail > 0 && panelE > 0.01f && panelE < 0.98f && !ui.reducedMotion) {
				for (int i = 2; i >= 1; i--) {
					float ghost = (float) (trail * 0.22 * (1 - panelE) / i) * saved;
					Draw2D.rounded(g, contentX - 4 + (1 - panelE) * 18 * (1 + i * 0.9f), contentY - 4, contentW + 8, contentH + 8, 6,
						Colors.fade(ui.accentDarkOr(), ghost));
				}
			}
			ui.alpha = saved * panelE;
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate((1 - panelE) * 18, 0);
			boolean live = panelIn.isOpening();
			panel.render(g, ui, contentX, contentY - 2, contentW, contentH + 2, live ? mx : -1000, live ? my : -1000, query);
			pose.popMatrix();
			if (live) {
				String tip = panel.tooltip(mx, my, ui);
				if (tip != null) {
					tooltip = tip;
				}
				Row row = panel.list.rowAt(mx, my, ui);
				if (row != null && row.overResetIcon(mx, my)) {
					tooltip = "Reset " + row.setting().name() + " to its default";
				} else if (row != null && !ui.descriptions && row.setting() != null && !row.setting().description().isEmpty()) {
					tooltip = row.setting().description();
				}
			}
		}
		ui.alpha = saved;
	}

	private void collectCards(String query) {
		cards.clear();
		List<Module> source = category == null ? Hollow.modules().all() : Hollow.modules().byCategory(category);
		for (Module module : source) {
			if (query.isEmpty() || module.matches(query)) {
				cards.add(module);
			}
		}
		if (cardFocus >= cards.size()) {
			cardFocus = cards.size() - 1;
		}
	}

	private void renderGrid(GuiGraphics g, int mx, int my, String query) {
		collectCards(query);
		String title = category == null ? "All Modules" : category.displayName();
		String subtitle;
		if (!query.isEmpty()) {
			subtitle = cards.size() + (cards.size() == 1 ? " module matches" : " modules match") + " \"" + query + "\"";
		} else if (category == null) {
			int on = 0;
			for (Module module : Hollow.modules().all()) {
				if (module.isToggleable() && module.isEnabled()) {
					on++;
				}
			}
			subtitle = Hollow.modules().all().size() + " modules · " + on + " on";
		} else {
			subtitle = category.description();
		}
		renderTitle(g, title, subtitle);

		gridTop = contentY + 30;
		int gridH = contentY + contentH - gridTop;
		columns = Math.max(1, (contentW + GAP) / (MIN_CARD + GAP));
		cardW = (contentW - GAP * (columns - 1)) / columns;
		cardH = ui.descriptions ? 58 : 36;
		int rows = (cards.size() + columns - 1) / columns;
		int contentHeight = rows * (cardH + GAP);
		double maxScroll = Math.max(0, contentHeight - gridH + 4);
		gridScrollTarget = Math.max(0, Math.min(maxScroll, gridScrollTarget));
		gridScroll = ui.approach(gridScroll, gridScrollTarget, 0.05);

		g.enableScissor(contentX - 4, gridTop - 2, contentX + contentW + 4, gridTop + gridH);
		for (int i = 0; i < cards.size(); i++) {
			int cx = contentX + (i % columns) * (cardW + GAP);
			int cy = gridTop + (i / columns) * (cardH + GAP) - (int) Math.round(gridScroll);
			if (cy + cardH < gridTop - 2 || cy > gridTop + gridH) {
				continue;
			}
			boolean inGrid = my >= gridTop && my < gridTop + gridH;
			renderCard(g, cards.get(i), cx, cy, inGrid ? mx : -1000, inGrid ? my : -1000, query, keyboardNav && i == cardFocus);
		}
		if (cards.isEmpty()) {
			String text = query.isEmpty() ? "No modules here" : category != null ? "Nothing in " + category.displayName()
				+ " matches. Pick All Modules to search everything." : "No module or setting matches \"" + query + "\"";
			Draw2D.centered(g, font, Draw2D.ellipsize(font, text, contentW), contentX + contentW / 2, gridTop + 20, ui.c(Theme.TEXT_DIM));
		}
		g.disableScissor();
		if (maxScroll > 0) {
			float barH = Math.max(16, (float) gridH * gridH / (contentHeight + gridH));
			float barY = gridTop + (float) ((gridH - barH) * (gridScroll / maxScroll));
			Draw2D.rounded(g, contentX + contentW + 2, barY, 2.5f, barH, 1.25f, ui.c(Colors.withAlpha(ui.accent, 160)));
		}
	}

	/** A card's description in at most two lines, the second ellipsized; cached per width. */
	private Wrapped description(Module module, int width) {
		Wrapped cached = wrapped.get(module);
		if (cached != null && cached.width() == width) {
			return cached;
		}
		String text = module.description();
		String first = font.plainSubstrByWidth(text, width);
		if (first.length() < text.length()) {
			int space = first.lastIndexOf(' ');
			if (space > 0) {
				first = first.substring(0, space);
			}
		}
		String rest = text.substring(first.length()).trim();
		List<String> lines = rest.isEmpty() ? List.of(first) : List.of(first, Draw2D.ellipsize(font, rest, width));
		cached = new Wrapped(width, lines, !rest.isEmpty() && font.width(rest) > width);
		wrapped.put(module, cached);
		return cached;
	}

	private int toggleX(int cx) {
		return cx + cardW - 8 - Toggle.WIDTH;
	}

	private boolean overCardToggle(Module module, int cx, int cy, double mx, double my) {
		return module.isToggleable() && module.isAvailable() && Pill.over(mx, my, toggleX(cx) - 4, cy + 3, Toggle.WIDTH + 8, 16);
	}

	private void renderCard(GuiGraphics g, Module module, int x, int y, int mx, int my, String query, boolean focused) {
		boolean hovered = Pill.over(mx, my, x, y, cardW, cardH);
		boolean on = module.isToggleable() && module.isEnabled();
		double[] anim = cardAnim.computeIfAbsent(module, m -> new double[] {0, on ? 1 : 0});
		anim[0] = ui.approach(anim[0], hovered ? 1 : 0, 0.05);
		anim[1] = ui.approach(anim[1], on ? 1 : 0, 0.06);
		float h = (float) anim[0];
		float a = (float) anim[1];

		int fill = Colors.lerp(Theme.CARD, Theme.CARD_HOVER, h);
		if (a > 0.01f) {
			fill = Colors.lerp(fill, Colors.lerp(Theme.CARD, ui.accent, 0.18f), a);
		}
		Draw2D.rounded(g, x, y, cardW, cardH, 4, ui.c(fill));
		int border = focused ? ui.detail : Colors.lerp(Theme.BORDER, ui.accent, Math.max(a * 0.6f, h * 0.5f));
		Draw2D.roundedBorder(g, x, y, cardW, cardH, 4, 1, ui.c(border));
		if (a > 0.01f) {
			Draw2D.rounded(g, x + 2, y + 6, 2, cardH - 12, 1, ui.c(Colors.withAlpha(ui.accentLight, (int) (255 * a))));
		}

		int nameRoom = cardW - 20 - (module.isToggleable() ? Toggle.WIDTH + 8 : 10);
		int nameColor = module.isAvailable() ? Theme.TEXT : Theme.TEXT_DIM;
		Draw2D.text(g, font, Draw2D.ellipsize(font, module.name(), nameRoom), x + 9, y + 7, ui.c(nameColor));
		if (module.isToggleable() && module.isAvailable()) {
			Toggle toggle = toggles.computeIfAbsent(module, m -> new Toggle(on, false));
			boolean overToggle = overCardToggle(module, x, y, mx, my);
			toggle.render(g, ui, toggleX(x), y + 6, on, overToggle ? 1 : 0);
			if (overToggle) {
				tooltip = (on ? "Turn off " : "Turn on ") + module.name() + " (Space)";
			}
		} else {
			Draw2D.text(g, font, "›", x + cardW - 12, y + 7, ui.c(Colors.lerp(Theme.TEXT_FAINT, ui.detail, h)));
		}

		// Status line: state, keybind, category (when mixed) and search matches.
		int sx = x + 9;
		int sy = y + 20;
		sx += Pill.status(g, ui, sx, sy, SettingsPanel.status(module), SettingsPanel.statusColor(module, ui)) + 4;
		if (module.isToggleable() && module.keybind().isBound()) {
			String key = KeyInput.describe(module.keybind().get());
			sx += Pill.status(g, ui, sx, sy, key.toUpperCase(Locale.ROOT), Theme.TEXT_DIM) + 4;
		}
		String extra = "";
		if (!query.isEmpty()) {
			int matches = 0;
			for (var setting : module.settings()) {
				if (setting.isVisible() && setting.matches(query)) {
					matches++;
				}
			}
			extra = matches == 0 ? "" : matches + (matches == 1 ? " setting" : " settings");
		} else if (category == null) {
			extra = module.category().displayName();
		}
		if (!extra.isEmpty() && sx < x + cardW - 20) {
			Draw2D.text(g, font, Draw2D.ellipsize(font, extra, x + cardW - 8 - sx), sx, sy + 1, ui.c(Theme.TEXT_FAINT));
		}

		Wrapped description = ui.descriptions ? description(module, cardW - 18) : null;
		if (description != null) {
			int ly = y + 34;
			for (String line : description.lines()) {
				Draw2D.text(g, font, line, x + 9, ly, ui.c(Theme.TEXT_FAINT));
				ly += 10;
			}
		}
		if (hovered && tooltip == null) {
			StringBuilder tip = new StringBuilder();
			if (description == null || description.truncated()) {
				tip.append(module.description());
			}
			module.unavailableReason().ifPresent(r -> tip.append(tip.isEmpty() ? "" : "\n").append("Unavailable: ").append(r));
			if (module.isEnabled()) {
				module.blockedReason().ifPresent(r -> tip.append(tip.isEmpty() ? "" : "\n").append("Paused: ").append(r));
			}
			if (!tip.isEmpty()) {
				tooltip = tip.toString();
			}
		}
	}

	private void renderFooter(GuiGraphics g) {
		int y = winY + winH - FOOTER + 4;
		long now = System.currentTimeMillis();
		String left;
		int color;
		if (!message.isEmpty() && now - messageAt < MESSAGE_MS) {
			left = message;
			float fade = Math.min(1f, (MESSAGE_MS - (now - messageAt)) / 600f);
			color = Colors.fade(ui.detail, fade);
		} else if (panel != null) {
			left = "Changed settings show a reset arrow on hover · Shift+scroll fine-tunes sliders · Esc back";
			color = Theme.TEXT_FAINT;
		} else if (page == Page.MODULES) {
			left = "Click a card for settings · its switch turns it on or off";
			color = Theme.TEXT_FAINT;
		} else {
			left = "Tab / arrows move · Enter selects · Esc closes";
			color = Theme.TEXT_FAINT;
		}
		String right = winW >= 520 ? "Ctrl+F search · Tab/arrows move · Enter open · Space toggle" : "";
		int rightW = right.isEmpty() ? 0 : font.width(right);
		if (rightW > 0 && font.width(left) + rightW + 30 > winW) {
			right = "";
			rightW = 0;
		}
		Draw2D.text(g, font, Draw2D.ellipsize(font, left, winW - 20 - rightW), winX + 10, y, ui.c(color));
		if (!right.isEmpty()) {
			Draw2D.text(g, font, right, winX + winW - 10 - rightW, y, ui.c(Colors.withAlpha(Theme.TEXT_FAINT, 170)));
		}
	}

	private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		if (tooltip == null || !tooltip.equals(shownTooltip)) {
			shownTooltip = tooltip;
			tooltipSince = now;
		}
		if (tooltip == null || now - tooltipSince < TOOLTIP_DELAY_MS) {
			return;
		}
		float fade = ui.reducedMotion ? 1f : Math.min(1f, (now - tooltipSince - TOOLTIP_DELAY_MS) / 150f);
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (String part : tooltip.split("\n")) {
			lines.addAll(font.split(FormattedText.of(part), 200));
		}
		int w = 0;
		for (FormattedCharSequence line : lines) {
			w = Math.max(w, font.width(line));
		}
		w += 12;
		int h = lines.size() * 10 + 8;
		int tx = Math.max(2, Math.min(mouseX + 10, width - w - 4));
		int ty = mouseY + 12 + h > height - 2 ? mouseY - h - 4 : mouseY + 12;
		ui.alpha = fade;
		Draw2D.rounded(g, tx, ty, w, h, 5, ui.c(0xF20B0910));
		Draw2D.roundedBorder(g, tx, ty, w, h, 5, 1, ui.c(Colors.withAlpha(ui.detail, 170)));
		int ly = ty + 5;
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, tx + 6, ly, ui.c(Theme.TEXT), false);
			ly += 10;
		}
		ui.alpha = 1f;
	}

	// ---------------------------------------------------------------- mouse

	private Row focusedRow() {
		RowList list = activeList();
		return list == null ? null : list.focusedRow();
	}

	private int cardIndexAt(double mx, double my) {
		if (panel != null || page != Page.MODULES || my < gridTop || my >= contentY + contentH) {
			return -1;
		}
		for (int i = 0; i < cards.size(); i++) {
			int cx = contentX + (i % columns) * (cardW + GAP);
			int cy = gridTop + (i / columns) * (cardH + GAP) - (int) Math.round(gridScroll);
			if (Pill.over(mx, my, cx, cy, cardW, cardH)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) {
			return true;
		}
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		keyboardNav = false;
		RowList list = activeList();
		if (list != null) {
			list.clearKeyboardFocus();
		}
		Row focused = focusedRow();
		if (focused != null && !focused.contains(mx, my, ui)) {
			focused.cancelInput();
		}
		boolean onSearch = overSearch(mx, my);
		search.focus(onSearch);
		if (onSearch) {
			if (button == 1) {
				search.setText("");
			}
			return true;
		}
		if (overClose(mx, my)) {
			onClose();
			return true;
		}
		if (overHud(mx, my)) {
			HudEditorScreen.open();
			return true;
		}
		if (overProfile(mx, my)) {
			select(navs.get(toolsStart + 1));
			return true;
		}
		if (overSidebarToggle(mx, my)) {
			settings().sidebarCollapsed.toggle();
			return true;
		}
		if (overSidebar(mx, my)) {
			Nav nav = navAt(mx, my);
			if (nav != null) {
				search.setText(nav.page() == page && nav.category() == category ? search.text() : "");
				select(nav);
			}
			return true;
		}
		if (panel != null && panelIn.isOpening()) {
			if (panel.overBack(mx, my, ui)) {
				closeSettings();
				return true;
			}
			if (panel.clickHeader(mx, my, ui)) {
				return true;
			}
		}
		if (list != null) {
			Row row = list.rowAt(mx, my, ui);
			if (row != null) {
				if (row.overResetIcon(mx, my) && row.setting() != null) {
					row.setting().reset();
					notify(row.setting().name() + " reset to default");
					return true;
				}
				activeRow = row;
				return row.mouseClicked(mx, my, button);
			}
			return true;
		}
		int index = cardIndexAt(mx, my);
		if (index >= 0) {
			Module module = cards.get(index);
			int cx = contentX + (index % columns) * (cardW + GAP);
			int cy = gridTop + (index / columns) * (cardH + GAP) - (int) Math.round(gridScroll);
			cardFocus = index;
			if (button == 0 && overCardToggle(module, cx, cy, mx, my)) {
				module.toggle();
			} else if (button == 0 || button == 1) {
				openSettings(module);
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
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
		if (overSidebar(mouseX, mouseY)) {
			sideScroll -= scrollY * navItemH;
			return true;
		}
		RowList list = activeList();
		if (list != null && list.contains(mouseX, mouseY)) {
			if (minecraft.hasShiftDown()) {
				Row row = list.rowAt(mouseX, mouseY, ui);
				if (row instanceof NumberRow number) {
					number.nudge(scrollY > 0 ? 1 : -1);
					return true;
				}
			}
			list.scrollBy(scrollY * 28);
			return true;
		}
		if (list == null && page == Page.MODULES) {
			gridScrollTarget -= scrollY * 30;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// ---------------------------------------------------------------- keyboard

	private static boolean isEnter(KeyEvent event) {
		return event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closing) {
			return true;
		}
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
			if (isEnter(event) || event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_TAB) {
				search.focus(false);
				keyboardNav = true;
				moveFocus(1, true);
				return true;
			}
			search.keyPressed(event);
			return true;
		}
		int key = event.key();
		if (key == GLFW.GLFW_KEY_F && event.hasControlDown()) {
			search.focus(true);
			return true;
		}
		if (event.isEscape()) {
			if (panel != null && panelIn.isOpening()) {
				closeSettings();
			} else if (!search.text().isEmpty()) {
				search.setText("");
			} else {
				onClose();
			}
			return true;
		}
		if (HollowClient.openMenuKey() != null && HollowClient.openMenuKey().matches(event)) {
			onClose();
			return true;
		}
		if (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN
			|| event.hasControlDown() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
			cyclePage(key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
			return true;
		}
		RowList list = activeList();
		if (list != null) {
			return listKey(event, list) || super.keyPressed(event);
		}
		return gridKey(event) || super.keyPressed(event);
	}

	private void cyclePage(int direction) {
		int current = 0;
		for (int i = 0; i < navs.size(); i++) {
			if (selected(navs.get(i))) {
				current = i;
			}
		}
		int count = navs.size() - 1; // the HUD editor opens another screen, so it is skipped
		int next = ((current + direction) % count + count) % count;
		keyboardNav = true;
		select(navs.get(next));
	}

	private void moveFocus(int direction, boolean fromSearch) {
		RowList list = activeList();
		if (list != null) {
			list.moveFocus(direction, ui);
			return;
		}
		if (cards.isEmpty()) {
			return;
		}
		cardFocus = fromSearch || cardFocus < 0 ? 0 : Math.max(0, Math.min(cards.size() - 1, cardFocus + direction));
		ensureCardVisible();
	}

	private void ensureCardVisible() {
		int row = cardFocus / columns;
		int top = row * (cardH + GAP);
		int gridH = contentY + contentH - gridTop;
		if (top < gridScrollTarget) {
			gridScrollTarget = top;
		} else if (top + cardH > gridScrollTarget + gridH) {
			gridScrollTarget = top + cardH - gridH + 4;
		}
	}

	private boolean listKey(KeyEvent event, RowList list) {
		int key = event.key();
		keyboardNav = true;
		Row row = list.keyboardRow();
		switch (key) {
			case GLFW.GLFW_KEY_TAB -> list.moveFocus(event.hasShiftDown() ? -1 : 1, ui);
			case GLFW.GLFW_KEY_DOWN -> list.moveFocus(1, ui);
			case GLFW.GLFW_KEY_UP -> list.moveFocus(-1, ui);
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
				if (row != null) {
					row.adjust(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
				}
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
				if (row != null) {
					row.activate();
				}
			}
			case GLFW.GLFW_KEY_DELETE -> {
				if (row != null && row.setting() != null && row.setting().persistent()) {
					row.setting().reset();
					notify(row.setting().name() + " reset to default");
				}
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (panel != null) {
					closeSettings();
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private boolean gridKey(KeyEvent event) {
		int key = event.key();
		boolean wasNav = keyboardNav;
		keyboardNav = true;
		switch (key) {
			case GLFW.GLFW_KEY_TAB -> moveFocus(event.hasShiftDown() ? -1 : 1, false);
			case GLFW.GLFW_KEY_RIGHT -> moveFocus(1, false);
			case GLFW.GLFW_KEY_LEFT -> moveFocus(-1, false);
			case GLFW.GLFW_KEY_DOWN -> moveFocus(columns, false);
			case GLFW.GLFW_KEY_UP -> moveFocus(-columns, false);
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (cardFocus >= 0 && cardFocus < cards.size()) {
					openSettings(cards.get(cardFocus));
				}
			}
			case GLFW.GLFW_KEY_SPACE -> {
				if (cardFocus >= 0 && cardFocus < cards.size() && cards.get(cardFocus).isToggleable()) {
					cards.get(cardFocus).toggle();
				}
			}
			default -> {
				keyboardNav = wasNav;
				return false;
			}
		}
		return true;
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

	// ---------------------------------------------------------------- lifecycle

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
		Row focused = focusedRow();
		if (focused != null) {
			focused.cancelInput();
		}
		closing = true;
		open.duration(seconds(0.8));
		open.close();
		if (reducedMotion()) {
			open.snap(false);
			minecraft.setScreen(parent);
		}
	}

	@Override
	public void removed() {
		if (Hollow.config() != null) {
			Hollow.config().save();
		}
	}
}
