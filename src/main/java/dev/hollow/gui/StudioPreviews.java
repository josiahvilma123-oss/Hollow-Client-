package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.character.CharacterView;
import dev.hollow.character.Cosmetic;
import dev.hollow.core.util.Colors;
import dev.hollow.menu.MenuBackgrounds;
import dev.hollow.menu.MenuScene;
import dev.hollow.menu.PixelFont;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.LoadingScreenModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.modules.ThemePacksModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import dev.hollow.sky.SkyEnums.BodyStyle;
import dev.hollow.sky.SkyEnums.Silhouettes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Preview cards for the Studio and menu modules. Pure 2D sketches, except the live player model. */
final class StudioPreviews {
	private static final int CARD_GAP = 4;

	private StudioPreviews() {
	}

	private static double seconds() {
		return System.nanoTime() / 1e9;
	}

	/** A flat sketch of the current sky: gradient, stars, nebula, moon style and silhouettes. */
	static void sky(GuiGraphics g, UiContext ui, SkyStudioModule sky, int x, int y, int w, int h) {
		int sx = x + 6;
		int sy = y + 16;
		int sw = w - 12;
		int sh = h - 30;
		int horizonY = sy + sh * 3 / 4;
		g.fillGradient(sx, sy, sx + sw, horizonY, ui.c(sky.zenith.argb()), ui.c(sky.horizon.argb()));
		g.fillGradient(sx, horizonY, sx + sw, sy + sh, ui.c(sky.horizon.argb()), ui.c(sky.ground.argb()));
		if (sky.glowStrength.get() > 0) {
			g.fillGradient(sx, horizonY - sh / 5, sx + sw, horizonY, 0,
				ui.c(Colors.withAlpha(sky.horizonGlow.argb(), (int) (sky.glowStrength.get() * 200))));
		}
		if (sky.nebula.get()) {
			int a = (int) Math.min(120, sky.nebulaIntensity.get() * 120);
			Draw2D.rounded(g, sx + sw * 0.15f, sy + sh * 0.3f, sw * 0.5f, sh * 0.18f, sh * 0.09f, ui.c(Colors.withAlpha(sky.nebulaA.argb(), a)));
			Draw2D.rounded(g, sx + sw * 0.45f, sy + sh * 0.2f, sw * 0.35f, sh * 0.14f, sh * 0.07f, ui.c(Colors.withAlpha(sky.nebulaB.argb(), a)));
		}
		int stars = (int) (sky.starDensity.get() * 60);
		double t = seconds();
		for (int i = 0; i < stars; i++) {
			int px = sx + (int) ((i * 73L + 11) % sw);
			int py = sy + (int) ((i * 37L + 5) % Math.max(1, horizonY - sy - 4));
			float twinkle = (float) (0.6 + 0.4 * Math.sin(t * 2 + i) * sky.twinkle.get());
			g.fill(px, py, px + 1, py + 1, ui.c(Colors.withAlpha(sky.starColor.argb(), (int) (220 * twinkle))));
		}
		float mx = sx + sw * 0.72f;
		float my = sy + sh * 0.3f;
		float r = (float) Math.max(3, Math.min(sh * 0.16, sky.moonSize.get() * 1.5));
		BodyStyle style = sky.moonStyle.get();
		int moon = sky.moonColor.argb();
		if (style != BodyStyle.HIDDEN) {
			if (sky.moonGlow.get() > 0 || style == BodyStyle.HALO || style == BodyStyle.ECLIPSE) {
				Draw2D.circle(g, mx, my, r * 2f, ui.c(Colors.withAlpha(moon, (int) (40 + sky.moonGlow.get() * 50))));
			}
			Draw2D.circle(g, mx, my, r, ui.c(moon));
			if (style == BodyStyle.ECLIPSE) {
				Draw2D.circle(g, mx, my, r * 0.86f, ui.c(sky.zenith.argb()));
			} else if (style == BodyStyle.CRESCENT) {
				Draw2D.circle(g, mx + r * 0.4f, my - r * 0.15f, r * 0.85f, ui.c(Colors.lerp(sky.zenith.argb(), sky.horizon.argb(), 0.2f)));
			}
		}
		if (sky.ring.get()) {
			for (int i = 0; i < 36; i++) {
				double a0 = i * Math.PI / 18;
				double a1 = (i + 1) * Math.PI / 18;
				float rr = r * 2.2f;
				Draw2D.line(g, mx + (float) Math.cos(a0) * rr, my + (float) Math.sin(a0) * rr * 0.35f, mx + (float) Math.cos(a1) * rr,
					my + (float) Math.sin(a1) * rr * 0.35f, 1f, ui.c(Colors.withAlpha(sky.ringColor.argb(), 200)));
			}
		}
		if (sky.silhouettes.get() != Silhouettes.NONE) {
			int c = ui.c(sky.silhouetteColor.argb());
			for (int i = 0; i < 4; i++) {
				int bx = sx + sw * (i * 2 + 1) / 9;
				int base = horizonY + 1;
				switch (sky.silhouettes.get()) {
					case PYRAMIDS -> {
						for (int row = 0; row < 10; row++) {
							g.fill(bx - row, base - 10 + row, bx + row + 1, base - 9 + row, c);
						}
					}
					case OBELISKS -> g.fill(bx - 1, base - 12, bx + 2, base, c);
					default -> g.fill(bx, base - 14, bx + 1, base, c);
				}
			}
		}
		Draw2D.border(g, sx, sy, sw, sh, ui.c(Theme.BORDER_SOFT));
		String state = sky.isEnabled() ? sky.preset.get() + (sky.modified() ? " (modified)" : "") : "Off · vanilla sky";
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, state, w - 12), x + 6, y + h - 11, ui.c(sky.isEnabled() ? ui.accentLight : Theme.TEXT_DIM));
	}

	private static float studioYaw = 25f;
	private static boolean dragging;
	private static double dragLastX;

	private static float seconds3d() {
		return (float) (seconds() % 10000.0);
	}

	/** Geometry of the Character Studio card: one large figure and a gallery of every outfit. */
	private record StudioLayout(int bigX, int bigY, int bigW, int bigH, int tilesX, int tilesY, int tileW, int tileH, int columns) {
		int tileX(int index) {
			return tilesX + (index % columns) * (tileW + CARD_GAP);
		}

		int tileY(int index) {
			return tilesY + (index / columns) * (tileH + CARD_GAP);
		}
	}

	private static StudioLayout studioLayout(int x, int y, int w, int h) {
		int bigH = h - 32;
		int bigW = Math.max(60, Math.min(CharacterView.widthFor(bigH) + 20, w / 3));
		int tilesX = x + 6 + bigW + 8;
		int available = x + w - 6 - tilesX;
		int count = Cosmetic.values().length;
		int columns = available >= 5 * 44 ? 5 : 3;
		int rows = (count + columns - 1) / columns;
		int tileW = (available - CARD_GAP * (columns - 1)) / columns;
		int tileH = (h - 30 - CARD_GAP * (rows - 1)) / rows;
		return new StudioLayout(x + 6, y + 14, bigW, bigH, tilesX, y + 14, tileW, tileH, columns);
	}

	/**
	 * Your skin wearing the selected outfit as a large 3D figure (drag to turn it), next to a
	 * gallery of every outfit; clicking a tile wears it.
	 */
	static void character(GuiGraphics g, UiContext ui, CharacterStudioModule studio, int x, int y, int w, int h, int mouseX, int mouseY) {
		Cosmetic selected = studio.cosmetic.get();
		StudioLayout l = studioLayout(x, y, w, h);
		float t = seconds3d();
		g.fillGradient(l.bigX(), l.bigY(), l.bigX() + l.bigW(), l.bigY() + l.bigH(), ui.c(0xFF0B0812),
			ui.c(Colors.lerp(0xFF0B0812, selected.accent(), 0.3f)));
		int figureH = l.bigH() - 4;
		int figureW = Math.min(l.bigW(), CharacterView.widthFor(figureH));
		int fx = l.bigX() + (l.bigW() - figureW) / 2;
		float lookYaw = Math.max(-35f, Math.min(35f, (mouseX - (fx + figureW / 2f)) * 0.5f));
		CharacterView.draw(g, selected, fx, l.bigY() + 2, fx + figureW, l.bigY() + 2 + figureH, studioYaw, -4f, dragging ? 0f : lookYaw, 0f, t, 1f);
		Draw2D.roundedBorder(g, l.bigX(), l.bigY(), l.bigW(), l.bigH(), 4, 1, ui.c(Theme.BORDER_SOFT));

		Cosmetic hovered = null;
		Cosmetic[] all = Cosmetic.values();
		for (int i = 0; i < all.length; i++) {
			Cosmetic cosmetic = all[i];
			int tx = l.tileX(i);
			int ty = l.tileY(i);
			boolean over = mouseX >= tx && mouseX < tx + l.tileW() && mouseY >= ty && mouseY < ty + l.tileH();
			if (over) {
				hovered = cosmetic;
			}
			boolean isSelected = cosmetic == selected;
			g.fillGradient(tx, ty, tx + l.tileW(), ty + l.tileH(), ui.c(over ? 0xFF1A1424 : 0xFF100C16),
				ui.c(Colors.lerp(0xFF100C16, cosmetic.accent(), isSelected ? 0.4f : 0.18f)));
			int th = l.tileH() - 4;
			int tw = Math.min(l.tileW(), CharacterView.widthFor(th));
			CharacterView.draw(g, cosmetic, tx + (l.tileW() - tw) / 2, ty + 2, tx + (l.tileW() + tw) / 2, ty + 2 + th, 20f, -4f, 0f, 0f,
				t + i * 0.7f, 0.6f);
			int border = isSelected ? Colors.lerp(cosmetic.accent(), 0xFFFFFFFF, 0.25f) : over ? Theme.TEXT_FAINT : Theme.BORDER_SOFT;
			Draw2D.roundedBorder(g, tx, ty, l.tileW(), l.tileH(), 4, isSelected ? 2 : 1, ui.c(border));
		}

		Cosmetic named = hovered != null ? hovered : selected;
		String label = named.displayName() + (named == Cosmetic.NONE ? " (just your skin)" : "");
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, label, l.bigW() + 60), x + 6, y + h - 12,
			ui.c(Colors.lerp(named.accent(), 0xFFFFFFFF, 0.35f)));
		String hint = studio.isEnabled() ? "Drag to turn · Click a character to wear it · Only you see it" : "Turn the module on to wear it in game";
		int hintX = x + w - 6 - ui.font.width(hint);
		if (hintX > x + 6 + ui.font.width(label) + 8) {
			Draw2D.text(g, ui.font, hint, hintX, y + h - 12, ui.c(studio.isEnabled() ? Theme.TEXT_FAINT : Theme.DANGER));
		}
	}

	/** Handles a click on the Character Studio card; returns true when it was used. */
	static boolean characterClicked(CharacterStudioModule studio, int x, int y, int w, int h, double mouseX, double mouseY) {
		StudioLayout l = studioLayout(x, y, w, h);
		if (mouseX >= l.bigX() && mouseX < l.bigX() + l.bigW() && mouseY >= l.bigY() && mouseY < l.bigY() + l.bigH()) {
			dragging = true;
			dragLastX = mouseX;
			return true;
		}
		Cosmetic[] all = Cosmetic.values();
		for (int i = 0; i < all.length; i++) {
			int tx = l.tileX(i);
			int ty = l.tileY(i);
			if (mouseX >= tx && mouseX < tx + l.tileW() && mouseY >= ty && mouseY < ty + l.tileH()) {
				studio.cosmetic.set(all[i]);
				if (all[i] != Cosmetic.NONE) {
					studio.setEnabled(true);
				}
				return true;
			}
		}
		return false;
	}

	static boolean characterDragged(double mouseX) {
		if (!dragging) {
			return false;
		}
		studioYaw += (float) (mouseX - dragLastX) * 2.5f;
		dragLastX = mouseX;
		return true;
	}

	static void characterReleased() {
		dragging = false;
	}

	private static final int PACK_COLUMNS = 4;

	private static int packTileW(int w) {
		return (w - 12 - CARD_GAP * (PACK_COLUMNS - 1)) / PACK_COLUMNS;
	}

	private static int packTileH(int h) {
		int rows = (ThemePacksModule.Pack.values().length + PACK_COLUMNS - 1) / PACK_COLUMNS;
		return (h - 30 - CARD_GAP * (rows - 1)) / rows;
	}

	/** Every collection as a card with its character in 3D; the selected one is outlined. Clicking a card selects it. */
	static void themePacks(GuiGraphics g, UiContext ui, ThemePacksModule packs, int x, int y, int w, int h, int mouseX, int mouseY) {
		int cw = packTileW(w);
		int ch = packTileH(h);
		float t = seconds3d();
		ThemePacksModule.Pack hovered = null;
		for (ThemePacksModule.Pack pack : ThemePacksModule.Pack.values()) {
			int i = pack.ordinal();
			int cx = x + 6 + (i % PACK_COLUMNS) * (cw + CARD_GAP);
			int cy = y + 14 + (i / PACK_COLUMNS) * (ch + CARD_GAP);
			boolean selected = packs.pack.get() == pack;
			boolean over = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
			if (over) {
				hovered = pack;
			}
			g.fillGradient(cx, cy, cx + cw, cy + ch, ui.c(0xFF0B0812), ui.c(Colors.lerp(0xFF0B0812, pack.accent(), selected ? 0.45f : 0.25f)));
			int fh = ch - 13;
			int fw = Math.min(cw, CharacterView.widthFor(fh));
			CharacterView.draw(g, pack.cosmetic(), cx + (cw - fw) / 2, cy + 2, cx + (cw + fw) / 2, cy + 2 + fh, 20f, -4f, 0f, 0f, t + i * 0.7f, 0.6f);
			Draw2D.centered(g, ui.font, Draw2D.ellipsize(ui.font, pack.cosmetic().displayName(), cw - 4), cx + cw / 2, cy + ch - 10,
				ui.c(selected ? 0xFFFFFFFF : Theme.TEXT_DIM));
			int border = selected ? Colors.lerp(pack.accent(), 0xFFFFFFFF, 0.2f) : over ? Theme.TEXT_FAINT : Theme.BORDER_SOFT;
			Draw2D.roundedBorder(g, cx, cy, cw, ch, 4, selected ? 2 : 1, ui.c(border));
		}
		ThemePacksModule.Pack shown = hovered != null ? hovered : packs.pack.get();
		String status = hovered == null && !packs.lastApplied().isEmpty() ? packs.lastApplied() : shown.displayName() + " — " + shown.description();
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, status, w - 12), x + 6, y + h - 12, ui.c(Theme.TEXT_DIM));
	}

	/** Returns the pack under the cursor, or null. */
	static ThemePacksModule.Pack packAt(int x, int y, int w, int h, double mouseX, double mouseY) {
		int cw = packTileW(w);
		int ch = packTileH(h);
		for (ThemePacksModule.Pack pack : ThemePacksModule.Pack.values()) {
			int i = pack.ordinal();
			int cx = x + 6 + (i % PACK_COLUMNS) * (cw + CARD_GAP);
			int cy = y + 14 + (i / PACK_COLUMNS) * (ch + CARD_GAP);
			if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch) {
				return pack;
			}
		}
		return null;
	}

	/** A miniature of the title screen: the actual menu scene, scaled down, with the logo and buttons sketched in. */
	static void mainMenu(GuiGraphics g, UiContext ui, MainMenuModule menu, int x, int y, int w, int h) {
		int sx = x + 6;
		int sy = y + 14;
		int sw = w - 12;
		int sh = h - 28;
		Minecraft minecraft = Minecraft.getInstance();
		int screenW = minecraft.getWindow().getGuiScaledWidth();
		int screenH = minecraft.getWindow().getGuiScaledHeight();
		float scale = Math.min(sw / (float) screenW, sh / (float) screenH);
		int mw = (int) (screenW * scale);
		int mh = (int) (screenH * scale);
		int ox = sx + (sw - mw) / 2;
		g.enableScissor(ox, sy, ox + mw, sy + mh);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(ox, sy);
		pose.scale(scale, scale);
		if (menu.scene.get() == MainMenuModule.Scene.SKY) {
			// The Sky Studio pass draws full-screen and can't be scaled; sketch it instead.
			pose.popMatrix();
			g.disableScissor();
			sky(g, ui, Hollow.module(SkyStudioModule.class), ox - 6, sy - 16, mw + 12, mh + 30);
			return;
		}
		g.fill(0, 0, screenW, screenH, 0xFF000000);
		MenuScene.render(g, screenW, screenH, -1, -1, screenW / 2 + 128);
		int accent = menu.accent.argb();
		int logoW = 90;
		g.fill(screenW / 2 - logoW / 2, screenH / 8, screenW / 2 + logoW / 2, screenH / 8 + 28, Colors.withAlpha(accent, 140));
		for (int i = 0; i < 5; i++) {
			int by = screenH / 4 + 44 + i * 22;
			Draw2D.rounded(g, screenW / 2f - 100, by, 200, 20, 10, 0xD0120E19);
			Draw2D.roundedBorder(g, screenW / 2f - 100, by, 200, 20, 10, 1, Colors.withAlpha(accent, 160));
		}
		pose.popMatrix();
		g.disableScissor();
		Draw2D.border(g, ox, sy, mw, mh, ui.c(Theme.BORDER_SOFT));
		String state = menu.isEnabled() ? menu.scene.get().displayName() : "Off · vanilla title screen";
		if (menu.isEnabled() && menu.scene.get() == MainMenuModule.Scene.CUSTOM && !MenuBackgrounds.error().isEmpty()) {
			state = MenuBackgrounds.error() + " · showing the temple";
		}
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, state, w - 12), x + 6, y + h - 11, ui.c(menu.isEnabled() ? ui.accentLight : Theme.TEXT_DIM));
	}

	/** The loading ring and logo, looping through 0–100%. */
	static void loading(GuiGraphics g, UiContext ui, LoadingScreenModule loading, int x, int y, int w, int h) {
		int sx = x + 6;
		int sy = y + 14;
		int sw = w - 12;
		int sh = h - 28;
		g.fillGradient(sx, sy, sx + sw, sy + sh, ui.c(0xFF06040A), ui.c(0xFF150A21));
		double t = seconds() * loading.speed.get();
		float progress = (float) ((t / 3.0) % 1.2);
		progress = Math.min(1f, progress);
		float cx = sx + sw / 2f;
		float cy = sy + sh / 2f - 4;
		float r = Math.min(sw, sh) * 0.3f;
		if (loading.ring.get()) {
			for (int i = 0; i < 48; i++) {
				double a0 = -Math.PI / 2 + i * Math.PI / 24;
				double a1 = a0 + Math.PI / 24;
				boolean filled = (i + 1) / 48f <= progress;
				Draw2D.line(g, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r, cx + (float) Math.cos(a1) * r,
					cy + (float) Math.sin(a1) * r, filled ? 1.5f : 1f, ui.c(filled ? 0xFFC9A0FF : 0x509D4EDD));
			}
			if (loading.motionTrail.get() && !loading.reducedMotion.get()) {
				double spin = t * 1.4;
				for (int i = 0; i < 8; i++) {
					double a = spin - i * 0.08;
					Draw2D.circle(g, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 1.6f - i * 0.15f,
						ui.c(Colors.withAlpha(0xFFF0E2FF, 230 - i * 25)));
				}
			}
		}
		PixelFont.drawCentered(g, "HOLO", (int) cx, (int) cy - 5, 2, 2, ui.c(0xFFF4EEFF));
		int percent = Math.round(progress * 100);
		PixelFont.draw(g, "LOADING", sx + 8, sy + sh - 10, 1, 1, ui.c(0xFFCFC3E2));
		String p = percent + "%";
		PixelFont.draw(g, p, sx + sw - 8 - PixelFont.width(p, 1, 1), sy + sh - 10, 1, 1, ui.c(0xFFE6B54A));
		Draw2D.border(g, sx, sy, sw, sh, ui.c(Theme.BORDER_SOFT));
		String state = loading.isEnabled() ? (loading.quickHandoff.get() ? "Quick hand-off to the menu" : "Standard fade to the menu")
			: "Off · vanilla loading screen";
		Draw2D.text(g, ui.font, state, x + 6, y + h - 11, ui.c(loading.isEnabled() ? ui.accentLight : Theme.TEXT_DIM));
	}
}
