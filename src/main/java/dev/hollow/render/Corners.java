package dev.hollow.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hollow.Hollow;
import dev.hollow.core.util.Colors;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Rounded rectangles and outlines from anti-aliased circle and ring textures baked at the exact
 * pixel radius. A shape is four corner quads plus three or four plain fills, whatever its
 * radius, where drawing corners row by row costs dozens of fills per shape.
 */
final class Corners {
	/** Larger radii are clamped; nothing in Holo comes close. */
	private static final int MAX_RADIUS = 96;
	private static final Map<Integer, Identifier> TEXTURES = new HashMap<>();

	private Corners() {
	}

	/** Textures need the texture manager, and the first loading screen draws before it is ready. */
	static boolean usable() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getOverlay() == null && minecraft.getTextureManager() != null;
	}

	/** Circle (thickness 0) or ring texture with the given pixel radius, built on first use. */
	private static Identifier texture(int radius, int thickness) {
		int key = radius * 1000 + thickness;
		Identifier id = TEXTURES.get(key);
		if (id != null) {
			return id;
		}
		int size = radius * 2;
		NativeImage image = new NativeImage(size, size, false);
		float inner = thickness == 0 ? -1 : radius - thickness;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int inside = 0;
				for (int sy = 0; sy < 4; sy++) {
					for (int sx = 0; sx < 4; sx++) {
						float dx = x + (sx + 0.5f) / 4f - radius;
						float dy = y + (sy + 0.5f) / 4f - radius;
						float d = (float) Math.sqrt(dx * dx + dy * dy);
						if (d <= radius && d >= inner) {
							inside++;
						}
					}
				}
				image.setPixel(x, y, Colors.withAlpha(0xFFFFFFFF, inside * 255 / 16));
			}
		}
		id = Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "ui/corner_" + radius + "_" + thickness);
		DynamicTexture texture = new DynamicTexture(() -> "Holo corner", image) {
			{
				this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
			}
		};
		Minecraft.getInstance().getTextureManager().register(id, texture);
		TEXTURES.put(key, id);
		return id;
	}

	private static void corners(GuiGraphics g, Identifier texture, int px, int py, int pw, int ph, int r, int color) {
		int size = r * 2;
		g.blit(RenderPipelines.GUI_TEXTURED, texture, px, py, 0, 0, r, r, r, r, size, size, color);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, px + pw - r, py, r, 0, r, r, r, r, size, size, color);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, px, py + ph - r, 0, r, r, r, r, r, size, size, color);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, px + pw - r, py + ph - r, r, r, r, r, r, r, size, size, color);
	}

	static void rounded(GuiGraphics g, float x, float y, float w, float h, float radius, float scale, int color) {
		int px = Math.round(x * scale);
		int py = Math.round(y * scale);
		int pw = Math.round((x + w) * scale) - px;
		int ph = Math.round((y + h) * scale) - py;
		if (pw <= 0 || ph <= 0) {
			return;
		}
		int r = Math.min(MAX_RADIUS, (int) Math.min(radius * scale, Math.min(pw, ph) / 2f));
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(1f / scale, 1f / scale);
		if (r < 1) {
			g.fill(px, py, px + pw, py + ph, color);
		} else {
			corners(g, texture(r, 0), px, py, pw, ph, r, color);
			if (ph > r * 2) {
				g.fill(px, py + r, px + pw, py + ph - r, color);
			}
			if (pw > r * 2) {
				g.fill(px + r, py, px + pw - r, py + r, color);
				g.fill(px + r, py + ph - r, px + pw - r, py + ph, color);
			}
		}
		pose.popMatrix();
	}

	static void border(GuiGraphics g, float x, float y, float w, float h, float radius, float thickness, float scale, int color) {
		int px = Math.round(x * scale);
		int py = Math.round(y * scale);
		int pw = Math.round((x + w) * scale) - px;
		int ph = Math.round((y + h) * scale) - py;
		int t = Math.max(1, Math.round(thickness * scale));
		if (pw <= t * 2 || ph <= t * 2) {
			rounded(g, x, y, w, h, radius, scale, color);
			return;
		}
		int r = Math.min(MAX_RADIUS, (int) Math.min(radius * scale, Math.min(pw, ph) / 2f));
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(1f / scale, 1f / scale);
		if (r <= t) {
			// Too tight for a curve: square corners.
			g.fill(px, py, px + pw, py + t, color);
			g.fill(px, py + ph - t, px + pw, py + ph, color);
			g.fill(px, py + t, px + t, py + ph - t, color);
			g.fill(px + pw - t, py + t, px + pw, py + ph - t, color);
		} else {
			corners(g, texture(r, t), px, py, pw, ph, r, color);
			if (pw > r * 2) {
				g.fill(px + r, py, px + pw - r, py + t, color);
				g.fill(px + r, py + ph - t, px + pw - r, py + ph, color);
			}
			if (ph > r * 2) {
				g.fill(px, py + r, px + t, py + ph - r, color);
				g.fill(px + pw - t, py + r, px + pw, py + ph - r, color);
			}
		}
		pose.popMatrix();
	}
}
