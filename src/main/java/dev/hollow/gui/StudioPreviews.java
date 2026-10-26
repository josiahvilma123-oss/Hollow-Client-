package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.character.Cosmetic;
import dev.hollow.core.util.Colors;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;

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

	/** The local player in their cosmetic when in a world; the set's artwork otherwise. */
	static void character(GuiGraphics g, UiContext ui, CharacterStudioModule studio, int x, int y, int w, int h, int mouseX, int mouseY) {
		Cosmetic cosmetic = studio.cosmetic.get();
		Minecraft minecraft = Minecraft.getInstance();
		int boxX = x + 6;
		int boxW = Math.min(90, w / 2);
		if (minecraft.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, boxX, y + 14, boxX + boxW, y + h - 4, (h - 24) / 2, 0.0625f, mouseX, mouseY,
				minecraft.player);
		} else if (cosmetic != Cosmetic.NONE) {
			int ah = h - 18;
			int aw = ah * 504 / 648;
			g.blit(RenderPipelines.GUI_TEXTURED, MenuScene.art(cosmetic), boxX + (boxW - aw) / 2, y + 14, 0, 0, aw, ah, 504, 648, 504, 648);
		} else {
			Draw2D.centered(g, ui.font, "No set", boxX + boxW / 2, y + h / 2, ui.c(Theme.TEXT_FAINT));
		}
		int tx = boxX + boxW + 8;
		Draw2D.text(g, ui.font, cosmetic.displayName(), tx, y + 18, ui.c(Colors.lerp(cosmetic.accent(), 0xFFFFFFFF, 0.3f)));
		String line1 = minecraft.player != null ? "Live preview: move the mouse" : "Join a world for a live 3D preview";
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, line1, w - tx + x - 6), tx, y + 32, ui.c(Theme.TEXT_DIM));
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, "Visible only to you", w - tx + x - 6), tx, y + 44, ui.c(Theme.TEXT_FAINT));
		if (!studio.isEnabled()) {
			Draw2D.text(g, ui.font, "Module off", tx, y + 56, ui.c(Theme.DANGER));
		}
	}

	static int packCardWidth(int w) {
		return (w - 12 - CARD_GAP * 2) / 3;
	}

	/** Three collection cards; the selected one is outlined. Clicking a card selects it. */
	static void themePacks(GuiGraphics g, UiContext ui, ThemePacksModule packs, int x, int y, int w, int h, int mouseX, int mouseY) {
		int cw = packCardWidth(w);
		int ch = h - 30;
		int cy = y + 14;
		for (ThemePacksModule.Pack pack : ThemePacksModule.Pack.values()) {
			int cx = x + 6 + pack.ordinal() * (cw + CARD_GAP);
			boolean selected = packs.pack.get() == pack;
			boolean hovered = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
			g.fillGradient(cx, cy, cx + cw, cy + ch, ui.c(0xFF0B0812), ui.c(Colors.lerp(0xFF0B0812, pack.accent(), 0.35f)));
			int ah = ch - 14;
			int aw = ah * 504 / 648;
			g.blit(RenderPipelines.GUI_TEXTURED, MenuScene.art(pack.cosmetic()), cx + (cw - aw) / 2, cy + 2, 0, 0, aw, ah, 504, 648, 504, 648);
			Draw2D.centered(g, ui.font, Draw2D.ellipsize(ui.font, pack.cosmetic().displayName(), cw - 4), cx + cw / 2, cy + ch - 11,
				ui.c(selected ? 0xFFFFFFFF : Theme.TEXT_DIM));
			int border = selected ? Colors.lerp(pack.accent(), 0xFFFFFFFF, 0.2f) : hovered ? Theme.TEXT_FAINT : Theme.BORDER_SOFT;
			Draw2D.roundedBorder(g, cx, cy, cw, ch, 4, selected ? 2 : 1, ui.c(border));
		}
		String status = packs.lastApplied().isEmpty() ? packs.pack.get().description() : packs.lastApplied();
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, status, w - 12), x + 6, y + h - 12, ui.c(Theme.TEXT_DIM));
	}

	/** Returns the pack under the cursor, or null. */
	static ThemePacksModule.Pack packAt(int x, int y, int w, int h, double mouseX, double mouseY) {
		int cw = packCardWidth(w);
		int cy = y + 14;
		if (mouseY < cy || mouseY >= cy + h - 30) {
			return null;
		}
		for (ThemePacksModule.Pack pack : ThemePacksModule.Pack.values()) {
			int cx = x + 6 + pack.ordinal() * (cw + CARD_GAP);
			if (mouseX >= cx && mouseX < cx + cw) {
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
