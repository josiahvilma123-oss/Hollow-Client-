package dev.hollow.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hollow.Hollow;
import dev.hollow.character.Cosmetic;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.MainMenuModule.Featured;
import dev.hollow.modules.MainMenuModule.Scene;
import dev.hollow.render.Draw2D;
import dev.hollow.sky.HoloSkyRenderer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The animated backdrop shared by the Holo title screen and every out-of-world menu: a layered
 * night scene (sky, stars, moon, silhouettes, fog, particles) plus the featured character.
 * All state is static and preallocated, so drawing a frame allocates nothing, and nothing moves
 * while no menu is on screen because time only advances inside {@link #render}.
 */
public final class MenuScene {
	private static final Identifier TEMPLE = id("textures/gui/menu/temple.png");
	private static final Identifier DUNES = id("textures/gui/menu/dunes.png");
	private static final Identifier GLOW = id("menu/glow");
	private static final int STARS = 170;
	private static final float[] STAR_X = new float[STARS];
	private static final float[] STAR_Y = new float[STARS];
	private static final float[] STAR_SIZE = new float[STARS];
	private static final float[] STAR_PHASE = new float[STARS];
	private static final AmbientParticles PARTICLES = new AmbientParticles();
	private static final Identifier[] ART = new Identifier[Cosmetic.values().length];

	private static DynamicTexture glowTexture;
	private static long lastFrame = -1;
	private static float time;
	private static Scene shown;
	private static float dip;
	private static boolean skyFailed;

	static {
		for (Cosmetic cosmetic : Cosmetic.values()) {
			ART[cosmetic.ordinal()] = id("textures/gui/characters/" + (cosmetic == Cosmetic.NONE ? "wraith" : cosmetic.asset()) + ".png");
		}
		Random random = new Random(1337);
		for (int i = 0; i < STARS; i++) {
			STAR_X[i] = random.nextFloat();
			// Denser toward the zenith.
			STAR_Y[i] = (float) Math.pow(random.nextFloat(), 1.6) * 0.78f;
			STAR_SIZE[i] = random.nextFloat() < 0.12f ? 1.5f : random.nextFloat() < 0.5f ? 1f : 0.5f;
			STAR_PHASE[i] = random.nextFloat() * 6.28f;
		}
	}

	private MenuScene() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Hollow.MOD_ID, path);
	}

	/** Artwork for a character set. */
	public static Identifier art(Cosmetic cosmetic) {
		return ART[cosmetic.ordinal()];
	}

	/** Animation clock in seconds, already scaled by the animation intensity setting. */
	public static float time() {
		return time;
	}

	/** The scene that will actually be drawn: custom images fall back to the temple until they load. */
	private static Scene effectiveScene(MainMenuModule m) {
		Scene scene = m.scene.get();
		if (scene == Scene.CUSTOM && MenuBackgrounds.custom(m.customImage.get()) == null) {
			return Scene.TEMPLE;
		}
		if (scene == Scene.SKY && skyFailed) {
			return Scene.VOID;
		}
		return scene;
	}

	public static Cosmetic featured(MainMenuModule m) {
		Featured featured = m.character.get();
		return switch (featured) {
			case NONE -> Cosmetic.NONE;
			case FOLLOW_STUDIO -> {
				CharacterStudioModule studio = Hollow.module(CharacterStudioModule.class);
				yield studio.isEnabled() ? studio.cosmetic.get() : Cosmetic.NONE;
			}
			case PHARAOH -> Cosmetic.PHARAOH;
			case WRAITH -> Cosmetic.WRAITH;
			case HOLLOW_KNIGHT -> Cosmetic.HOLLOW_KNIGHT;
		};
	}

	/**
	 * Draws the full backdrop.
	 *
	 * @param characterLeftBound GUI x the featured character must stay right of, or -1 to hide it
	 */
	public static void render(GuiGraphics g, int w, int h, int mouseX, int mouseY, int characterLeftBound) {
		MainMenuModule m = Hollow.module(MainMenuModule.class);
		long now = System.nanoTime();
		float delta = lastFrame < 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		float motion = m.animation.getFloat();
		time += delta * motion;
		float lighting = m.lighting.getFloat();
		int accent = m.accent.argb();

		// Scene changes dip briefly through black instead of popping.
		Scene target = effectiveScene(m);
		if (shown == null) {
			shown = target;
		}
		if (target != shown) {
			dip = Math.min(1f, dip + delta * 6f);
			if (dip >= 1f || delta == 0f) {
				shown = target;
			}
		} else {
			dip = Math.max(0f, dip - delta * 4f);
		}

		float px = mouseX < 0 ? 0f : (mouseX / (float) Math.max(1, w) - 0.5f) * Math.min(1f, motion);
		float py = mouseY < 0 ? 0f : (mouseY / (float) Math.max(1, h) - 0.5f) * Math.min(1f, motion);

		switch (shown) {
			case SKY -> drawStudioSky(g, w, h, px, py);
			case CUSTOM -> drawCustom(g, m, w, h, px, py);
			default -> drawNight(g, shown, w, h, px, py, accent, lighting, m.decorations.get());
		}

		// Low fog in the scene's accent.
		g.fillGradient(0, (int) (h * 0.72f), w, h, 0x00000000, Colors.withAlpha(Colors.lerp(accent, 0xFF000000, 0.65f), (int) (90 * Math.min(1f, lighting))));

		Cosmetic cosmetic = featured(m);
		if (characterLeftBound >= 0 && cosmetic != Cosmetic.NONE) {
			drawCharacter(g, cosmetic, w, h, characterLeftBound, px, lighting);
		}
		if (m.particles.get()) {
			PARTICLES.render(g, w, h, m.particleCount.getInt(), motion, delta, time, accent, 1f);
		}
		if (m.decorations.get()) {
			g.fillGradient(0, 0, w, (int) (h * 0.18f), 0x90000000, 0x00000000);
			g.fillGradient(0, (int) (h * 0.86f), w, h, 0x00000000, 0xA0000000);
		}
		if (dip > 0.001f) {
			g.fill(0, 0, w, h, Colors.withAlpha(0xFF000000, (int) (dip * 255)));
		}
	}

	private static void drawNight(GuiGraphics g, Scene scene, int w, int h, float px, float py, int accent, float lighting, boolean decorations) {
		int top;
		int bottom;
		switch (scene) {
			case PYRAMIDS -> {
				top = 0xFF05040C;
				bottom = 0xFF3A1A34;
			}
			case VOID -> {
				top = 0xFF020104;
				bottom = 0xFF13071F;
			}
			default -> {
				top = 0xFF06030C;
				bottom = Colors.lerp(0xFF241034, accent, 0.18f);
			}
		}
		g.fillGradient(0, 0, w, h, top, bottom);

		float drift = time * 0.004f;
		// Nebula washes.
		int nebulaAlpha = (int) ((scene == Scene.VOID ? 70 : 34) * Math.min(1.2f, lighting));
		glow(g, w * (0.25f + 0.03f * (float) Math.sin(drift * 6)) - px * 8, h * 0.3f - py * 5, w * 0.7f, h * 0.45f,
			Colors.withAlpha(accent, nebulaAlpha));
		glow(g, w * 0.78f - px * 8, h * (0.18f + 0.02f * (float) Math.cos(drift * 5)) - py * 5, w * 0.55f, h * 0.35f,
			Colors.withAlpha(0xFF2B55C8, nebulaAlpha * 2 / 3));
		if (scene == Scene.VOID) {
			glow(g, w * 0.5f - px * 12, h * 0.62f - py * 8, w * 0.9f, h * 0.5f, Colors.withAlpha(0xFF7B1FA2, nebulaAlpha));
		}

		// Stars, twinkling and slowly wheeling.
		for (int i = 0; i < STARS; i++) {
			float sx = ((STAR_X[i] + drift) % 1f) * w - px * 6;
			float sy = STAR_Y[i] * h - py * 4;
			float twinkle = 0.55f + 0.45f * (float) Math.sin(time * 1.3f + STAR_PHASE[i]);
			int a = (int) (230 * twinkle * (0.4f + STAR_SIZE[i] * 0.4f));
			int color = i % 9 == 0 ? Colors.withAlpha(0xFFE9D7FF, a) : Colors.withAlpha(0xFFFFFFFF, a);
			float s = STAR_SIZE[i];
			Draw2D.line(g, sx - s / 2, sy, sx + s / 2, sy, s, color);
		}

		// The moon: an eclipse with a violet corona over the temple, a pale moon over the dunes.
		if (scene != Scene.VOID) {
			float mx = w * 0.2f - px * 10;
			float my = h * 0.24f - py * 6;
			float r = Math.max(10f, Math.min(w, h) * 0.07f);
			if (scene == Scene.TEMPLE) {
				glow(g, mx, my, r * 7, r * 7, Colors.withAlpha(accent, (int) (110 * Math.min(1.3f, lighting))));
				glow(g, mx, my, r * 3.2f, r * 3.2f, Colors.withAlpha(Colors.lerp(accent, 0xFFFFFFFF, 0.5f), (int) (200 * Math.min(1f, lighting))));
				disc(g, mx, my, r * 1.06f, Colors.withAlpha(Colors.lerp(accent, 0xFFFFFFFF, 0.7f), 230));
				disc(g, mx, my, r, 0xFF050308);
			} else {
				glow(g, mx, my, r * 6, r * 6, Colors.withAlpha(0xFFE8D9B0, (int) (70 * Math.min(1.3f, lighting))));
				disc(g, mx, my, r, 0xFFEDE3CF);
				disc(g, mx + r * 0.32f, my - r * 0.18f, r * 0.82f, Colors.withAlpha(top, 235));
			}
			if (decorations) {
				// Slowly sweeping light rays from the moon.
				for (int i = 0; i < 5; i++) {
					double angle = 0.55 + i * 0.16 + Math.sin(time * 0.15 + i) * 0.05;
					float length = h * 1.1f;
					Draw2D.line(g, mx, my, mx + (float) Math.cos(angle) * length, my + (float) Math.sin(angle) * length, r * (0.6f + i % 2),
						Colors.withAlpha(accent, (int) (14 * lighting)));
				}
			}
		}

		// Silhouettes, with stronger parallax than the sky.
		if (scene == Scene.TEMPLE || scene == Scene.PYRAMIDS) {
			boolean temple = scene == Scene.TEMPLE;
			int texHeight = temple ? 768 : 512;
			int drawWidth = (int) (w * 1.08f);
			int drawHeight = drawWidth * texHeight / 2048;
			int maxHeight = (int) (h * (temple ? 0.62f : 0.5f));
			if (drawHeight > maxHeight) {
				drawHeight = maxHeight;
				drawWidth = drawHeight * 2048 / texHeight;
			}
			int x = (w - drawWidth) / 2 - (int) (px * 16);
			int y = h - drawHeight + 2 - (int) (py * 6);
			g.blit(RenderPipelines.GUI_TEXTURED, temple ? TEMPLE : DUNES, x, y, 0, 0, drawWidth, drawHeight, 2048, texHeight, 2048, texHeight);
			if (x > 0) {
				g.fill(0, y + drawHeight - 4, x, h, 0xFF120E19);
			}
			if (x + drawWidth < w) {
				g.fill(x + drawWidth, y + drawHeight - 4, w, h, 0xFF120E19);
			}
		}
	}

	private static void drawStudioSky(GuiGraphics g, int w, int h, float px, float py) {
		if (!HoloSkyRenderer.INSTANCE.shaderUsable()) {
			skyFailed = true;
			g.fill(0, 0, w, h, 0xFF050308);
			return;
		}
		try {
			HoloSkyRenderer.INSTANCE.renderMenuSky(time * 1.2f + px * 6f, -14f + py * 4f, 75f);
		} catch (RuntimeException e) {
			// A broken custom texture or driver issue must never take the menu down.
			skyFailed = true;
			Hollow.LOGGER.warn("Sky Studio menu background failed; using the Void Realm scene instead", e);
			g.fill(0, 0, w, h, 0xFF050308);
		}
	}

	private static void drawCustom(GuiGraphics g, MainMenuModule m, int w, int h, float px, float py) {
		Identifier texture = MenuBackgrounds.custom(m.customImage.get());
		if (texture == null) {
			g.fill(0, 0, w, h, 0xFF050308);
			return;
		}
		int iw = MenuBackgrounds.width();
		int ih = MenuBackgrounds.height();
		// Cover the screen, keeping the aspect ratio, with a little headroom for parallax.
		float scale = Math.max(w * 1.04f / iw, h * 1.04f / ih);
		int dw = Math.round(iw * scale);
		int dh = Math.round(ih * scale);
		int x = (w - dw) / 2 - (int) (px * 10);
		int y = (h - dh) / 2 - (int) (py * 6);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, dw, dh, iw, ih, iw, ih);
		g.fill(0, 0, w, h, 0x40000000);
	}

	private static void drawCharacter(GuiGraphics g, Cosmetic cosmetic, int w, int h, int leftBound, float px, float lighting) {
		int drawHeight = (int) Math.min(h * 0.66f, 300);
		int drawWidth = drawHeight * 504 / 648;
		// Shrink to the space right of the menu column; hide when there's too little room.
		int room = w - leftBound - 6;
		if (drawWidth > room) {
			drawWidth = room;
			drawHeight = drawWidth * 648 / 504;
		}
		if (drawWidth < 70) {
			return;
		}
		float centerX = Math.max(w * 0.8f, leftBound + drawWidth * 0.5f);
		centerX = Math.min(centerX, w - drawWidth * 0.5f);
		float bob = (float) Math.sin(time * 0.8f) * 3f;
		float x = centerX - drawWidth / 2f - px * 22;
		float y = h - drawHeight - h * 0.04f + bob;
		glow(g, centerX - px * 22, y + drawHeight * 0.45f, drawWidth * 1.5f, drawHeight * 1.1f,
			Colors.withAlpha(cosmetic.accent(), (int) (80 * Math.min(1.5f, lighting))));
		Identifier art = ART[cosmetic.ordinal()];
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		g.blit(RenderPipelines.GUI_TEXTURED, art, 0, 0, 0, 0, drawWidth, drawHeight, 504, 648, 504, 648);
		pose.popMatrix();
	}

	/** An anti-aliased disc, drawn as one textured quad pair. */
	private static void disc(GuiGraphics g, float x, float y, float radius, int color) {
		int size = Math.round(radius * 2);
		Pill.fill(g, Math.round(x - radius), Math.round(y - radius), size, size, color);
	}

	/** A soft radial glow centered at (x, y), tinted by {@code color}. */
	public static void glow(GuiGraphics g, float x, float y, float width, float height, int color) {
		if (Colors.alpha(color) <= 0) {
			return;
		}
		Identifier texture = glowTexture();
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x - width / 2f, y - height / 2f);
		pose.scale(width / 64f, height / 64f);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 64, 64, 128, 128, 128, 128, color);
		pose.popMatrix();
	}

	private static Identifier glowTexture() {
		if (glowTexture == null) {
			NativeImage image = new NativeImage(128, 128, false);
			for (int y = 0; y < 128; y++) {
				for (int x = 0; x < 128; x++) {
					float dx = (x + 0.5f - 64) / 64f;
					float dy = (y + 0.5f - 64) / 64f;
					float d = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy));
					float falloff = (1 - d) * (1 - d);
					falloff = falloff * falloff * (3 - 2 * falloff);
					image.setPixel(x, y, Colors.withAlpha(0xFFFFFFFF, Math.round(falloff * 255)));
				}
			}
			glowTexture = new DynamicTexture(() -> "Holo menu glow", image) {
				{
					this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
				}
			};
			Minecraft.getInstance().getTextureManager().register(GLOW, glowTexture);
		}
		return GLOW;
	}

	/** Frees the textures this scene created; they are rebuilt on demand. */
	public static void release() {
		if (glowTexture != null) {
			Minecraft.getInstance().getTextureManager().release(GLOW);
			glowTexture = null;
		}
		MenuBackgrounds.release();
		Pill.release();
		shown = null;
	}
}
