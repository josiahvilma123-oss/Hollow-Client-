package dev.hollow.menu;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Drifting embers and motes for menu backgrounds. A fixed pool of particles is reused, so
 * rendering allocates nothing per frame.
 */
public final class AmbientParticles {
	private static final int MAX = 200;
	private final float[] x = new float[MAX];
	private final float[] y = new float[MAX];
	private final float[] speed = new float[MAX];
	private final float[] size = new float[MAX];
	private final float[] phase = new float[MAX];
	private final boolean[] warm = new boolean[MAX];
	private final Random random = new Random(42);

	public AmbientParticles() {
		for (int i = 0; i < MAX; i++) {
			respawn(i, random.nextFloat());
		}
	}

	private void respawn(int i, float height) {
		x[i] = random.nextFloat();
		y[i] = height;
		speed[i] = 0.01f + random.nextFloat() * 0.03f;
		size[i] = 0.6f + random.nextFloat() * 1.6f;
		phase[i] = random.nextFloat() * 6.28f;
		warm[i] = random.nextFloat() < 0.35f;
	}

	/**
	 * @param count     particles to draw (at most 200)
	 * @param motion    animation intensity; 0 freezes them
	 * @param delta     seconds since the last frame
	 */
	public void render(GuiGraphics g, int width, int height, int count, float motion, float delta, float time, int accent, float alpha) {
		int n = Math.min(MAX, Math.max(0, count));
		for (int i = 0; i < n; i++) {
			y[i] -= speed[i] * delta * motion;
			if (y[i] < -0.05f) {
				respawn(i, 1.05f);
			}
			float sway = (float) Math.sin(time * 0.6f + phase[i]) * 0.012f * motion;
			float px = (x[i] + sway) * width;
			float py = y[i] * height;
			float twinkle = 0.55f + 0.45f * (float) Math.sin(time * 1.7f + phase[i] * 3f);
			int color = warm[i] ? 0xFFE6B54A : Colors.lerp(accent, 0xFFFFFFFF, 0.3f);
			float fade = Math.min(1f, Math.min(y[i] * 4f, (1.05f - y[i]) * 3f));
			int a = Math.round(200 * twinkle * Math.max(0f, fade) * alpha);
			if (a <= 2) {
				continue;
			}
			// One soft sprite for the halo and one fill for the core: two GUI elements per particle.
			float halo = size[i] * 5f;
			MenuScene.glow(g, px, py, halo, halo, Colors.withAlpha(color, a / 3));
			float core = Math.max(1f, size[i] * 0.8f);
			Draw2D.line(g, px - core / 2, py, px + core / 2, py, core, Colors.withAlpha(color, a));
		}
	}
}
