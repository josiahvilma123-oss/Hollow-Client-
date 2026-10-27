package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.blur.MotionBlurMath;
import dev.hollow.core.module.Module;
import dev.hollow.core.util.Colors;
import dev.hollow.detection.EntityCategory;
import dev.hollow.hands.HandAnimator;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.LoadingScreenModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.modules.ThemePacksModule;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.modules.ServerRulesModule;
import dev.hollow.policy.ServerContext;
import dev.hollow.render.Draw2D;
import dev.hollow.render.HudOverlay;
import dev.hollow.render.Theme;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

/** Live preview card shown at the top of a module page. */
final class PreviewRow extends Row {
	private final Module module;

	private PreviewRow(Module module) {
		this.module = module;
	}

	static PreviewRow forModule(Module module) {
		return switch (module.id()) {
			case "motion_blur", "hollow_hands", "block_finder", "block_esp", "entity_tracers", "hollow_alerts", "server_rules",
				"sky_studio", "character_studio", "theme_packs", "main_menu", "loading_screen" ->
				new PreviewRow(module);
			default -> null;
		};
	}

	@Override
	int height(UiContext ui) {
		return switch (module.id()) {
			case "block_finder" -> 26 + legendHeight(ui);
			case "hollow_alerts" -> 54;
			case "server_rules" -> 58;
			case "sky_studio", "main_menu", "loading_screen" -> 104;
			case "character_studio", "theme_packs" -> 160;
			default -> 70;
		};
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		int h = height(ui) - 6;
		int cx = x + PAD;
		int cw = width - PAD * 2;
		Draw2D.panel(g, cx, y, cw, h, ui.c(0xFF0A080D));
		Draw2D.border(g, cx, y, cw, h, ui.c(Theme.BORDER_SOFT));
		Draw2D.spaced(g, ui.font, "PREVIEW", cx + 6, y + 5, ui.c(Theme.TEXT_FAINT), 1);
		g.enableScissor(cx + 1, y + 1, cx + cw - 1, y + h - 1);
		switch (module.id()) {
			case "motion_blur" -> motionBlur(g, ui, cx, y, cw, h);
			case "hollow_hands" -> hands(g, ui, cx, y, cw, h);
			case "block_finder" -> finder(g, ui, cx, y, cw);
			case "block_esp" -> esp(g, ui, cx, y, cw, h);
			case "entity_tracers" -> tracers(g, ui, cx, y, cw, h);
			case "hollow_alerts" -> alerts(g, ui, cx, y, cw);
			case "server_rules" -> rules(g, ui, cx, y);
			case "sky_studio" -> StudioPreviews.sky(g, ui, (SkyStudioModule) module, cx, y, cw, h);
			case "character_studio" -> StudioPreviews.character(g, ui, (CharacterStudioModule) module, cx, y, cw, h, mouseX, mouseY);
			case "theme_packs" -> StudioPreviews.themePacks(g, ui, (ThemePacksModule) module, cx, y, cw, h, mouseX, mouseY);
			case "main_menu" -> StudioPreviews.mainMenu(g, ui, (MainMenuModule) module, cx, y, cw, h);
			case "loading_screen" -> StudioPreviews.loading(g, ui, (LoadingScreenModule) module, cx, y, cw, h);
			default -> {
			}
		}
		g.disableScissor();
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (module instanceof CharacterStudioModule studio && button == 0) {
			return StudioPreviews.characterClicked(studio, x + PAD, y, width - PAD * 2, height(ui) - 6, mouseX, mouseY);
		}
		if (module instanceof ThemePacksModule packs && button == 0) {
			ThemePacksModule.Pack pack = StudioPreviews.packAt(x + PAD, y, width - PAD * 2, height(ui) - 6, mouseX, mouseY);
			if (pack != null) {
				packs.pack.set(pack);
				return true;
			}
		}
		return false;
	}

	@Override
	boolean mouseDragged(double mouseX, double mouseY) {
		return module instanceof CharacterStudioModule && StudioPreviews.characterDragged(mouseX);
	}

	@Override
	void mouseReleased() {
		StudioPreviews.characterReleased();
	}

	private static double seconds() {
		return System.nanoTime() / 1e9;
	}

	private void motionBlur(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		MotionBlurModule blur = (MotionBlurModule) module;
		double trail = blur.guiTrail();
		double period = Math.max(0.6, blur.guiDuration.get() / 1000.0 * 6);
		int trackX = x + 12;
		int trackW = w / 2 - 24;
		int trackY = y + 30;
		g.fill(trackX, trackY + 6, trackX + trackW, trackY + 7, ui.c(Theme.BORDER));
		double now = seconds();
		int ghosts = trail > 0 ? 5 : 0;
		for (int i = ghosts; i >= 0; i--) {
			double t = now - i * 0.022;
			double phase = (Math.sin(t / period * Math.PI * 2) + 1) / 2;
			double eased = dev.hollow.core.anim.Easing.CUBIC_IN_OUT.ease(phase);
			int bx = trackX + (int) Math.round(eased * (trackW - 22));
			float alpha = i == 0 ? 1f : (float) (trail * 0.45 * (1 - i / (double) (ghosts + 1)));
			Draw2D.panel(g, bx, trackY, 22, 13, ui.c(Colors.fade(i == 0 ? ui.accent : ui.accentLight, alpha)));
		}
		Draw2D.text(g, ui.font, trail > 0 ? "GUI trails on" : "GUI trails off", trackX, y + h - 12, ui.c(Theme.TEXT_DIM));
		int infoX = x + w / 2 + 4;
		String camera = blur.cameraActive() ? "Camera blur on" : "Camera blur off";
		Draw2D.text(g, ui.font, camera, infoX, y + 20, ui.c(blur.cameraActive() ? ui.accentLight : Theme.TEXT_DIM));
		double threshold = 360 + (30 - 360) * blur.sensitivity.get();
		Draw2D.text(g, ui.font, String.format(Locale.ROOT, "Starts above %.0f°/s", threshold), infoX, y + 32, ui.c(Theme.TEXT_DIM));
		double sample = MotionBlurMath.blendFactor(1, blur.intensity.get(), blur.duration.get(), 1 / 60.0);
		Draw2D.text(g, ui.font, String.format(Locale.ROOT, "Peak trail %.0f%% at 60 FPS", sample * 100), infoX, y + 44, ui.c(Theme.TEXT_DIM));
	}

	private void hands(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		HollowHandsModule hands = (HollowHandsModule) module;
		HandAnimator animator = new HandAnimator();
		animator.config().swingIntensity = hands.swingIntensity.get();
		// Swing curve: pitch of the arm over one swing.
		int gx = x + 12;
		int gy = y + 18;
		int gw = w / 2 - 24;
		int gh = h - 26;
		g.fill(gx, gy + gh, gx + gw, gy + gh + 1, ui.c(Theme.BORDER));
		float previousX = gx;
		float previousY = gy + gh;
		for (int i = 1; i <= 40; i++) {
			double t = i / 40.0;
			double eased = Math.min(0.999, hands.swingEasing.get().ease(t));
			double pitch = -animator.swingTransform(eased, true).pitch();
			float px = gx + (float) (t * gw);
			float py = gy + gh - (float) (pitch / 120.0 * gh);
			Draw2D.line(g, previousX, previousY, px, py, 1.2f, ui.c(ui.accent));
			previousX = px;
			previousY = py;
		}
		double cycle = 0.3 / hands.swingSpeed.get();
		double local = seconds() % (cycle + 0.45);
		if (local < cycle) {
			double t = local / cycle;
			double eased = Math.min(0.999, hands.swingEasing.get().ease(t));
			double pitch = -animator.swingTransform(eased, true).pitch();
			int dx = gx + (int) (t * gw);
			int dy = gy + gh - (int) (pitch / 120.0 * gh);
			Draw2D.glow(g, dx - 2, dy - 2, 4, 4, ui.c(ui.accent), 3, 1f);
			Draw2D.panel(g, dx - 2, dy - 2, 4, 4, ui.c(0xFFFFFFFF));
		}
		Draw2D.text(g, ui.font, "Swing · " + hands.swingEasing.get().displayName(), gx, y + h - 10, ui.c(Theme.TEXT_DIM));
		// Walk bob path, shaped like the in-game offsets.
		int bx = x + w * 3 / 4;
		int by = y + h / 2 + 2;
		double amount = hands.bob.get();
		previousX = Float.NaN;
		for (int i = 0; i <= 48; i++) {
			double phase = i / 48.0 * Math.PI * 2;
			float px = bx + (float) (Math.sin(phase) * 22 * amount);
			float py = by + (float) ((-Math.abs(Math.cos(phase)) + 0.5) * 22 * amount);
			if (!Float.isNaN(previousX)) {
				Draw2D.line(g, previousX, previousY, px, py, 1f, ui.c(Colors.withAlpha(ui.accentLight, 160)));
			}
			previousX = px;
			previousY = py;
		}
		double phase = seconds() * Math.PI * 1.8;
		int dotX = bx + (int) Math.round(Math.sin(phase) * 22 * amount);
		int dotY = by + (int) Math.round((-Math.abs(Math.cos(phase)) + 0.5) * 22 * amount);
		Draw2D.panel(g, dotX - 2, dotY - 2, 4, 4, ui.c(0xFFFFFFFF));
		Draw2D.text(g, ui.font, "Walk bob", bx - 20, y + h - 10, ui.c(Theme.TEXT_DIM));
	}

	private int legendHeight(UiContext ui) {
		BlockFinderModule finder = (BlockFinderModule) module;
		int count = 0;
		for (BlockFinderModule.CategorySettings settings : finder.categories().values()) {
			if (settings.isOn()) {
				count++;
			}
		}
		int perLine = Math.max(1, (width - PAD * 2 - 12) / 128);
		return Math.max(1, (count + perLine - 1) / perLine) * 12 + 14;
	}

	private void finder(GuiGraphics g, UiContext ui, int x, int y, int w) {
		BlockFinderModule finder = (BlockFinderModule) module;
		Map<String, Integer> counts = Hollow.scanner() != null && finder.isEnabled() ? Hollow.scanner().countsByCategory() : Map.of();
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();
		String status;
		if (!finder.isEnabled()) {
			status = "Off · enable World Scanner to start scanning";
		} else if (!Hollow.module(ServerRulesModule.class).detectionAllowed()) {
			status = "Paused · this server is listed as restricted";
		} else {
			status = String.format(Locale.ROOT, "%d matches in loaded chunks within %.0fm", total, finder.radius.get());
		}
		Draw2D.text(g, ui.font, status, x + 6, y + 17, ui.c(finder.isEnabled() ? ui.accentLight : Theme.TEXT_DIM));
		int perLine = Math.max(1, (w - 12) / 128);
		int index = 0;
		for (Map.Entry<String, BlockFinderModule.CategorySettings> entry : finder.categories().entrySet()) {
			BlockFinderModule.CategorySettings settings = entry.getValue();
			if (!settings.isOn()) {
				continue;
			}
			int found = counts.getOrDefault(entry.getKey(), 0);
			int lx = x + 6 + index % perLine * 128;
			int ly = y + 30 + index / perLine * 12;
			Draw2D.panel(g, lx, ly + 1, 6, 6, ui.c(settings.color.argb()));
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, settings.name(), 96), lx + 10, ly, ui.c(Theme.TEXT));
			String n = Integer.toString(found);
			Draw2D.text(g, ui.font, n, lx + 120 - ui.font.width(n), ly, ui.c(Theme.TEXT_FAINT));
			index++;
		}
	}

	private void esp(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		BlockEspModule esp = (BlockEspModule) module;
		BlockFinderModule finder = Hollow.module(BlockFinderModule.class);
		int color = ui.accent;
		for (BlockFinderModule.CategorySettings settings : finder.categories().values()) {
			if (settings.isOn()) {
				color = settings.color.argb();
				break;
			}
		}
		// Isometric cube, slowly rotating.
		double angle = seconds() * 0.6;
		int cx = x + w * 2 / 3;
		int cy = y + h / 2 + 4;
		float size = 15;
		float[][] corners = new float[8][2];
		for (int i = 0; i < 8; i++) {
			double px = (i & 1) == 0 ? -1 : 1;
			double py = (i & 2) == 0 ? -1 : 1;
			double pz = (i & 4) == 0 ? -1 : 1;
			double rx = px * Math.cos(angle) - pz * Math.sin(angle);
			double rz = px * Math.sin(angle) + pz * Math.cos(angle);
			corners[i][0] = cx + (float) (rx * size);
			corners[i][1] = cy + (float) (py * size * 0.9 + rz * size * 0.35);
		}
		int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
		if (esp.style.get().fill()) {
			int fill = Colors.withAlpha(color, Math.round(esp.fillOpacity.getFloat() * 255));
			int minX = (int) Math.min(Math.min(corners[0][0], corners[1][0]), Math.min(corners[4][0], corners[5][0]));
			int maxX = (int) Math.max(Math.max(corners[0][0], corners[1][0]), Math.max(corners[4][0], corners[5][0]));
			for (int sx = minX; sx < maxX; sx += 2) {
				g.fill(sx, (int) (cy - size * 0.9f), sx + 2, (int) (cy + size * 0.9f), ui.c(fill));
			}
		}
		if (esp.style.get().outline()) {
			for (int[] edge : edges) {
				Draw2D.line(g, corners[edge[0]][0], corners[edge[0]][1], corners[edge[1]][0], corners[edge[1]][1],
					esp.lineWidth.getFloat() * 0.6f, ui.c(color));
			}
		}
		int originX = x + 30;
		int originY = y + h / 2 + 4;
		g.fill(originX - 3, originY, originX + 4, originY + 1, ui.c(Theme.TEXT));
		g.fill(originX, originY - 3, originX + 1, originY + 4, ui.c(Theme.TEXT));
		if (esp.tracers.get()) {
			Draw2D.line(g, originX, originY, cx, cy, esp.tracerWidth.getFloat() * 0.6f, ui.c(Colors.fade(color, esp.tracerOpacity.getFloat())));
		}
		if (esp.labels.get()) {
			String label = esp.merge.get() ? "Diamond Ore ×4" : "Diamond Ore";
			if (esp.distance.get()) {
				label += "  12.4m";
			}
			int lw = ui.font.width(label) + 8;
			Draw2D.panel(g, cx - lw / 2, cy - (int) size - 16, lw, 11, ui.c(0xB80B0910));
			Draw2D.text(g, ui.font, label, cx - lw / 2 + 4, cy - (int) size - 14, ui.c(Colors.lerp(color, 0xFFFFFFFF, 0.5f)));
		}
	}

	private void tracers(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		EntityTracersModule tracers = (EntityTracersModule) module;
		boolean players = Hollow.module(ServerRulesModule.class).playersAllowed();
		int ox = x + w / 2;
		int oy = y + h / 2 + 4;
		EntityCategory[] shown = {EntityCategory.HOSTILE, EntityCategory.PASSIVE, EntityCategory.NEUTRAL, EntityCategory.PLAYERS};
		// Two targets on each side, with room for their labels on the outer side.
		int radiusX = (int) Math.max(40, Math.min(w / 2.0 - 150, 160));
		int[][] offsets = {{-radiusX, -13}, {-radiusX + 18, 13}, {radiusX, -13}, {radiusX - 18, 13}};
		for (int i = 0; i < shown.length; i++) {
			EntityCategory category = shown[i];
			EntityTracersModule.CategorySettings settings = tracers.category(category);
			int breathe = (int) Math.round(Math.sin(seconds() * 1.3 + i) * 1.5);
			int tx = ox + offsets[i][0];
			int ty = oy + offsets[i][1] + breathe;
			boolean visible = settings.show.get() && (category != EntityCategory.PLAYERS || players);
			int color = visible ? settings.color.argb() : Theme.OFF;
			if (visible && tracers.lines.get()) {
				Draw2D.line(g, ox, oy, tx, ty, tracers.thickness.getFloat() * 0.6f, ui.c(Colors.fade(color, tracers.opacity.getFloat())));
			}
			Draw2D.panel(g, tx - 4, ty - 6, 8, 12, ui.c(visible ? color : Colors.withAlpha(Theme.OFF, 160)));
			if (tracers.boxes.get() && visible) {
				Draw2D.border(g, tx - 6, ty - 8, 12, 16, ui.c(color));
			}
			String label = category == EntityCategory.PLAYERS && !players ? "Players (not permitted)" : category.displayName();
			// Labels sit on the outer side of their marker so they never cross the tracer lines.
			int labelX = tx >= ox ? tx + 9 : tx - 9 - ui.font.width(label);
			Draw2D.text(g, ui.font, label, labelX, ty - 4, ui.c(visible ? Theme.TEXT : Theme.TEXT_FAINT));
		}
		g.fill(ox - 3, oy, ox + 4, oy + 1, ui.c(Theme.TEXT));
		g.fill(ox, oy - 3, ox + 1, oy + 4, ui.c(Theme.TEXT));
	}

	private void alerts(GuiGraphics g, UiContext ui, int x, int y, int w) {
		Alert sample = new Alert(AlertSource.BLOCKS, "Amethyst Cluster", "Detected", 12.4, "preview");
		int toastWidth = HudOverlay.toastWidth(ui.font, sample, 1);
		HudOverlay.drawSample(g, ui.font, sample, x + (w - toastWidth) / 2, y + 16, ui.alpha);
	}

	private void rules(GuiGraphics g, UiContext ui, int x, int y) {
		ServerRulesModule rules = (ServerRulesModule) module;
		ServerContext context = ServerContext.current();
		String where = context.singleplayer() ? "Single-player world" : context.address().orElse("Not connected");
		Draw2D.text(g, ui.font, "Connected to: " + where, x + 6, y + 17, ui.c(Theme.TEXT));
		boolean detection = rules.detectionAllowed();
		boolean players = rules.playersAllowed();
		Draw2D.text(g, ui.font, "Block & entity detection: " + (detection ? "allowed" : "off (restricted)"), x + 6, y + 28,
			ui.c(detection ? ui.accentLight : Theme.DANGER));
		Draw2D.text(g, ui.font, "Player detection: " + (players ? "allowed" : "off (not permitted)"), x + 6, y + 39,
			ui.c(players ? ui.accentLight : Theme.TEXT_DIM));
	}
}
