package dev.hollow.menu;

import dev.hollow.Hollow;
import dev.hollow.character.CharacterView;
import dev.hollow.character.Cosmetic;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.LoadingScreenModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.render.Draw2D;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the Holo loading screen inside Minecraft's own loading overlay. Progress is the real
 * reload progress the overlay reports, and the overlay keeps its own timing: nothing here delays
 * the hand-over to the menu.
 */
public final class HoloLoadingScreen {
	private static final int ACCENT = 0xFF9D4EDD;
	private static final int GOLD = 0xFFE6B54A;
	private static final String[] PERCENT = new String[101];
	private static final String LOGO = "HOLO";

	static {
		for (int i = 0; i <= 100; i++) {
			PERCENT[i] = i + "%";
		}
	}

	private final boolean reload;
	private final AmbientParticles particles = new AmbientParticles();
	private long start = -1;
	private long lastFrame = -1;
	private float spin;

	public HoloLoadingScreen(boolean reload) {
		this.reload = reload;
	}

	public static boolean active() {
		return Hollow.modules() != null && Hollow.module(LoadingScreenModule.class).isEnabled();
	}

	private static float ringRadius(int w, int h) {
		return Math.max(24f, Math.min(w, h) * 0.17f);
	}

	private static float centerY(int h) {
		return h * 0.44f;
	}

	/** Background, ring, logo and particles. {@code alpha} is the overlay's own fade. */
	public void renderScene(GuiGraphics g, int w, int h, float progress, float alpha) {
		LoadingScreenModule m = Hollow.module(LoadingScreenModule.class);
		long now = System.nanoTime();
		if (start < 0) {
			start = now;
		}
		float delta = lastFrame < 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		float speed = m.speed.getFloat();
		boolean reduced = m.reducedMotion.get();
		float t = (now - start) / 1e9f * speed;
		if (!reduced) {
			spin += delta * speed * 1.4f;
		}

		g.fillGradient(0, 0, w, h, Colors.fade(0xFF06040A, alpha), Colors.fade(0xFF150A21, alpha));
		float cx = w / 2f;
		float cy = centerY(h);
		float r = ringRadius(w, h);
		float pulse = reduced ? 1f : 0.85f + 0.15f * (float) Math.sin(t * 2.2f);
		MenuScene.glow(g, cx, cy, r * 5.5f, r * 4.2f, Colors.fade(Colors.withAlpha(ACCENT, (int) (95 * pulse)), alpha));

		if (reload && m.character.get()) {
			drawCharacter(g, w, h, alpha, t, reduced);
		}
		if (m.particles.get() && !reduced) {
			particles.render(g, w, h, 45, speed, delta, t, ACCENT, alpha);
		}
		if (m.ring.get()) {
			drawRing(g, cx, cy, r, progress, alpha, m.motionTrail.get() && !reduced);
		}
		drawLogo(g, cx, cy, r, t, reduced, alpha);
	}

	private void drawRing(GuiGraphics g, float cx, float cy, float r, float progress, float alpha, boolean trail) {
		int segments = 72;
		double step = Math.PI * 2 / segments;
		double top = -Math.PI / 2;
		for (int i = 0; i < segments; i++) {
			double a0 = top + i * step;
			double a1 = a0 + step;
			boolean filled = (i + 1) / (float) segments <= progress + 0.0001f;
			int color = filled ? Colors.lerp(ACCENT, 0xFFFFFFFF, 0.35f) : Colors.withAlpha(ACCENT, 55);
			Draw2D.line(g, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r,
				cx + (float) Math.cos(a1) * r, cy + (float) Math.sin(a1) * r, filled ? 2f : 1f, Colors.fade(color, alpha));
		}
		// Twelve hour-marks, like a sundial.
		for (int i = 0; i < 12; i++) {
			double a = top + i * Math.PI / 6;
			float inner = r + 4;
			float outer = r + (i % 3 == 0 ? 9 : 6);
			Draw2D.line(g, cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner,
				cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer, 1f, Colors.fade(Colors.withAlpha(GOLD, i % 3 == 0 ? 200 : 110), alpha));
		}
		if (trail) {
			// A comet circling the ring, its tail fading behind it.
			for (int i = 0; i < 14; i++) {
				double a = spin - i * 0.07;
				double b = a - 0.07;
				float fade = 1f - i / 14f;
				Draw2D.line(g, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, cx + (float) Math.cos(b) * r,
					cy + (float) Math.sin(b) * r, 2.5f * fade + 0.5f, Colors.fade(Colors.withAlpha(0xFFF0E2FF, (int) (230 * fade)), alpha));
			}
			Draw2D.circle(g, cx + (float) Math.cos(spin) * r, cy + (float) Math.sin(spin) * r, 2.2f, Colors.fade(0xFFFFFFFF, alpha));
		}
	}

	private void drawLogo(GuiGraphics g, float cx, float cy, float r, float t, boolean reduced, float alpha) {
		int pixel = Math.max(2, Math.round(r / 9f));
		int spacing = pixel;
		int width = PixelFont.width(LOGO, pixel, spacing);
		int x = Math.round(cx - width / 2f);
		int y = Math.round(cy - pixel * 2.5f);
		int letterWidth = 3 * pixel + spacing;
		for (int i = 0; i < LOGO.length(); i++) {
			// Letters rise into place one after another; reduced motion only fades them.
			float local = reduced ? Math.min(1f, t / 0.6f) : clamp01((t - 0.12f * i) / 0.45f);
			float eased = local * local * (3 - 2 * local);
			int lift = reduced ? 0 : Math.round((1 - eased) * pixel * 3);
			String letter = LOGO.substring(i, i + 1);
			int lx = x + i * letterWidth;
			PixelFont.draw(g, letter, lx + 1, y + lift + 1, pixel, spacing, Colors.fade(Colors.withAlpha(ACCENT, 160), eased * alpha));
			PixelFont.draw(g, letter, lx, y + lift, pixel, spacing, Colors.fade(0xFFF4EEFF, eased * alpha));
		}
	}

	/** The featured character in 3D. It can't fade, so it only shows once the overlay is mostly opaque. */
	private static void drawCharacter(GuiGraphics g, int w, int h, float alpha, float time, boolean reduced) {
		Cosmetic cosmetic = MenuScene.featured(Hollow.module(MainMenuModule.class));
		if (cosmetic == null || w < 420 || alpha < 0.75f) {
			return;
		}
		int drawHeight = (int) Math.min(h * 0.5f, 220);
		int drawWidth = CharacterView.widthFor(drawHeight);
		int x = (int) (w * 0.82f - drawWidth / 2f);
		int y = (int) (h * 0.92f - drawHeight);
		float yaw = reduced ? 20f : 20f + (float) Math.sin(time * 0.4f) * 15f;
		CharacterView.draw(g, cosmetic, x, y, x + drawWidth, y + drawHeight, yaw, -4f, 0f, 0f, time, reduced ? 0f : 1f);
	}

	/** Status line and progress bar. {@code alpha} fades them out once loading completes. */
	public void renderProgress(GuiGraphics g, int w, int h, float progress, boolean done, float alpha) {
		float r = ringRadius(w, h);
		int barWidth = Math.round(Math.min(w * 0.6f, r * 3.2f));
		int x = Math.round(w / 2f - barWidth / 2f);
		int y = Math.round(centerY(h) + r + 26);
		int pixel = h >= 300 ? 2 : 1;
		String status = done ? "READY" : reload ? "RELOADING RESOURCES" : "LOADING RESOURCES";
		String percent = PERCENT[Math.round(clamp01(progress) * 100)];
		int textColor = Colors.fade(0xFFCFC3E2, alpha);
		PixelFont.draw(g, status, x, y, pixel, pixel, textColor);
		PixelFont.draw(g, percent, x + barWidth - PixelFont.width(percent, pixel, pixel), y, pixel, pixel, Colors.fade(GOLD, alpha));
		int barY = y + pixel * 5 + 5;
		Draw2D.rounded(g, x, barY, barWidth, 4, 2, Colors.fade(0xFF221A2E, alpha));
		float fill = barWidth * clamp01(progress);
		if (fill >= 1f) {
			Draw2D.rounded(g, x, barY, fill, 4, 2, Colors.fade(Colors.lerp(ACCENT, 0xFFFFFFFF, 0.2f), alpha));
		}
	}

	private static float clamp01(float v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}
}
