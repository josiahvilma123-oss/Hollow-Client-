package dev.hollow.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hollow.Hollow;
import dev.hollow.core.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Pill shapes (fully rounded ends) drawn from two small anti-aliased textures: a disc for the
 * fill and a ring for the outline. A pill is a few quads, instead of the per-row spans
 * {@code Draw2D.rounded} uses, which matters on screens with many buttons.
 */
public final class Pill {
	private static final int SIZE = 64;
	private static final Identifier DISC = Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "menu/pill_disc");
	private static final Identifier RING = Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "menu/pill_ring");
	private static boolean ready;

	private Pill() {
	}

	public static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
		draw(g, DISC, x, y, w, h, color, true);
	}

	/** A 1 GUI-pixel outline, sized for pills around 20 pixels tall. */
	public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
		draw(g, RING, x, y, w, h, color, false);
	}

	private static void draw(GuiGraphics g, Identifier texture, int x, int y, int w, int h, int color, boolean filled) {
		if (Colors.alpha(color) == 0 || w <= 0 || h <= 0) {
			return;
		}
		ensureTextures();
		int cap = Math.min(h / 2, w / 2);
		int half = SIZE / 2;
		// Left and right caps: the halves of the disc (or ring) texture.
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, cap, h, half, SIZE, SIZE, SIZE, color);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x + w - cap, y, half, 0, cap, h, half, SIZE, SIZE, SIZE, color);
		int middle = w - cap * 2;
		if (middle <= 0) {
			return;
		}
		if (filled) {
			g.fill(x + cap, y, x + cap + middle, y + h, color);
		} else {
			g.fill(x + cap, y, x + cap + middle, y + 1, color);
			g.fill(x + cap, y + h - 1, x + cap + middle, y + h, color);
		}
	}

	private static void ensureTextures() {
		if (ready) {
			return;
		}
		register(DISC, image(false));
		register(RING, image(true));
		ready = true;
	}

	private static void register(Identifier id, NativeImage image) {
		DynamicTexture texture = new DynamicTexture(() -> "Holo " + id.getPath(), image) {
			{
				this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
			}
		};
		Minecraft.getInstance().getTextureManager().register(id, texture);
	}

	/** White disc or ring with 4x4 supersampled edges. The ring is a twentieth of the diameter thick. */
	private static NativeImage image(boolean ring) {
		NativeImage image = new NativeImage(SIZE, SIZE, false);
		float radius = SIZE / 2f;
		float inner = radius - SIZE / 20f;
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int inside = 0;
				for (int sy = 0; sy < 4; sy++) {
					for (int sx = 0; sx < 4; sx++) {
						float dx = x + (sx + 0.5f) / 4f - radius;
						float dy = y + (sy + 0.5f) / 4f - radius;
						float d = (float) Math.sqrt(dx * dx + dy * dy);
						if (d <= radius && (!ring || d >= inner)) {
							inside++;
						}
					}
				}
				image.setPixel(x, y, Colors.withAlpha(0xFFFFFFFF, inside * 255 / 16));
			}
		}
		return image;
	}

	/** Frees the textures; they are rebuilt on next use. */
	public static void release() {
		if (ready) {
			Minecraft.getInstance().getTextureManager().release(DISC);
			Minecraft.getInstance().getTextureManager().release(RING);
			ready = false;
		}
	}
}
