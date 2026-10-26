package dev.hollow.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.hollow.Hollow;
import dev.hollow.compat.ShaderPackCompat;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.render.HollowPipelines;
import dev.hollow.render.ScreenProjector;
import dev.hollow.sky.SkyEnums.BodyStyle;
import dev.hollow.sky.SkyEnums.CloudMode;
import dev.hollow.sky.SkyEnums.Motion;
import dev.hollow.sky.SkyEnums.TimeMode;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Draws the Holo sky in a single full-screen pass, for the overworld sky and the main menu.
 * Parameters are repacked every frame into reused objects (no per-frame allocation); textures
 * are resolved once per change or resource reload, with a blank fallback for missing assets.
 */
public final class HoloSkyRenderer {
	public static final HoloSkyRenderer INSTANCE = new HoloSkyRenderer();
	private static final int UBO_SIZE;
	private static final Identifier BLANK_TEXTURE = Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "sky/blank");
	private static final Vector3f PLANET_A = new Vector3f(-0.55f, 0.45f, 0.70f).normalize();
	private static final Vector3f PLANET_B = new Vector3f(0.60f, 0.30f, -0.74f).normalize();

	static {
		Std140SizeCalculator size = new Std140SizeCalculator().putMat4f();
		for (int i = 0; i < 21; i++) {
			size.putVec4();
		}
		UBO_SIZE = size.get();
	}

	// Captured vanilla sky state for the current frame.
	private DimensionType.Skybox skybox = DimensionType.Skybox.NONE;
	private float sunAngle;
	private float moonAngle;
	private float starAngle;
	private float rainBrightness = 1;
	private int vanillaSkyColor = 0xFF78A7FF;
	private int moonPhaseIndex;

	private MappableRingBuffer uniforms;
	private boolean replacingThisFrame;
	private final Matrix4f inverseViewProjection = new Matrix4f();
	private final Matrix4f scratch = new Matrix4f();
	private final Vector3f sunDir = new Vector3f();
	private final Vector3f moonDir = new Vector3f();
	private final Vector4f horizonNow = new Vector4f();
	private final long startNanos = System.nanoTime();

	private String resolvedTextureId;
	private long resolvedGeneration = -1;
	private GpuTextureView textureView;
	private boolean textureValid;
	private long resourceGeneration;
	private DynamicTexture blank;
	private String lastWarning = "";

	private HoloSkyRenderer() {
	}

	private static SkyStudioModule module() {
		return Hollow.module(SkyStudioModule.class);
	}

	/** Called after vanilla extracts the sky state each frame. */
	public void capture(SkyRenderState state) {
		skybox = state.skybox;
		sunAngle = state.sunAngle;
		moonAngle = state.moonAngle;
		starAngle = state.starAngle;
		rainBrightness = state.rainBrightness;
		vanillaSkyColor = state.skyColor;
		moonPhaseIndex = state.moonPhase.index();
	}

	/** Called by the resource reloader; textures are re-resolved on next use. */
	public void onResourcesReloaded() {
		resourceGeneration++;
	}

	/**
	 * Only overworld-style skies are replaced. The End and dimensions without a sky keep vanilla
	 * behaviour, as does any frame where an Iris shader pack owns the sky.
	 */
	public boolean shouldReplaceWorldSky() {
		Minecraft minecraft = Minecraft.getInstance();
		return module().isEnabled() && minecraft.level != null && skybox == DimensionType.Skybox.OVERWORLD
			&& !ShaderPackCompat.shaderPackActive() && shaderUsable();
	}

	/** False (and Sky Studio marked unavailable) when this driver can't compile the sky shader. */
	public boolean shaderUsable() {
		if (HollowPipelines.usable(HollowPipelines.HOLO_SKY)) {
			return true;
		}
		if (module().isAvailable()) {
			module().markUnavailable("This graphics driver couldn't compile the sky shader; the vanilla sky is kept");
		}
		return false;
	}

	/** Decides once per frame, at the first sky draw call, whether vanilla's sky is replaced. */
	public boolean beginWorldSky() {
		replacingThisFrame = shouldReplaceWorldSky() && ScreenProjector.INSTANCE.valid();
		return replacingThisFrame;
	}

	public boolean replacingThisFrame() {
		return replacingThisFrame;
	}

	public void renderWorldSky() {
		try {
			ScreenProjector.INSTANCE.inverseViewProjection(inverseViewProjection);
			computeBodies(false, 0);
			draw(inverseViewProjection, false);
		} catch (RuntimeException e) {
			Hollow.LOGGER.error("Holo sky failed; restoring the vanilla sky", e);
			module().setEnabled(false);
		}
	}

	/**
	 * Draws the sky as a menu background with a slowly turning camera. The pass is issued
	 * immediately, before the menu's GUI is drawn on top.
	 */
	public void renderMenuSky(float yawDegrees, float pitchDegrees, float fovDegrees) {
		Minecraft minecraft = Minecraft.getInstance();
		int width = minecraft.getWindow().getWidth();
		int height = minecraft.getWindow().getHeight();
		scratch.identity().perspective((float) Math.toRadians(fovDegrees), width / (float) Math.max(1, height), 0.05f, 100f)
			.rotateX((float) Math.toRadians(pitchDegrees))
			.rotateY((float) Math.toRadians(yawDegrees));
		scratch.invert(inverseViewProjection);
		float seconds = (System.nanoTime() - startNanos) / 1e9f;
		computeBodies(true, seconds);
		draw(inverseViewProjection, true);
	}

	private void computeBodies(boolean menu, float seconds) {
		SkyStudioModule m = module();
		boolean pinned = menu || m.motion.get() == Motion.PINNED;
		if (pinned) {
			pinnedDirection(m.sunElevation.getFloat(), m.sunAzimuth.getFloat(), sunDir);
			pinnedDirection(m.moonElevation.getFloat(), m.moonAzimuth.getFloat(), moonDir);
		} else {
			sunDir.set((float) -Math.sin(sunAngle), (float) Math.cos(sunAngle), 0f);
			moonDir.set((float) -Math.sin(moonAngle), (float) Math.cos(moonAngle), 0f);
		}
		if (menu) {
			starAngle = 0.35f + seconds * 0.004f;
			rainBrightness = 1f;
		} else if (pinned) {
			starAngle = 0.35f;
		}
	}

	private static void pinnedDirection(float elevationDegrees, float azimuthDegrees, Vector3f out) {
		double e = Math.toRadians(elevationDegrees);
		double a = Math.toRadians(azimuthDegrees);
		out.set((float) (-Math.sin(a) * Math.cos(e)), (float) Math.sin(e), (float) (Math.cos(a) * Math.cos(e)));
	}

	private void draw(Matrix4f invViewProj, boolean menu) {
		SkyStudioModule m = module();
		RenderSystem.assertOnRenderThread();
		if (uniforms == null) {
			uniforms = new MappableRingBuffer(() -> "Holo sky uniforms", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UBO_SIZE);
		}
		boolean followGame = !menu && m.timeMode.get() == TimeMode.FOLLOW_GAME;
		float day = followGame ? smoothstep(-0.15f, 0.25f, sunDir.y) : 0f;
		float rain = menu ? 1f : rainBrightness;
		float visibility = (followGame ? clamp(1f - day * 1.25f, 0f, 1f) : 1f) * rain;
		int dayZenith = m.useVanillaDay.get() ? vanillaSkyColor | 0xFF000000 : m.dayZenith.argb();
		int dayHorizon = m.useVanillaDay.get() ? Colors.lerp(vanillaSkyColor | 0xFF000000, 0xFFFFFFFF, 0.4f) : m.dayHorizon.argb();
		int zenith = Colors.lerp(m.zenith.argb(), dayZenith, day);
		int horizon = Colors.lerp(m.horizon.argb(), dayHorizon, day);
		int ground = Colors.lerp(m.ground.argb(), Colors.lerp(dayHorizon, 0xFF000000, 0.5f), day);
		float sunset = followGame ? (float) Math.exp(-Math.abs(sunDir.y) * 8.0) : 1f;
		float glowStrength = m.glowStrength.getFloat() * (followGame ? 0.5f + sunset : 1f);
		float brightness = m.brightness.getFloat() * (0.55f + 0.45f * rain);
		horizonNow.set(Colors.red(horizon) / 255f, Colors.green(horizon) / 255f, Colors.blue(horizon) / 255f, 1f);

		int moonPhase = moonPhaseIndex;
		float illumination = (float) ((1 + Math.cos(moonPhase * Math.PI / 4)) / 2);
		float phase = (moonPhase <= 4 ? -1f : 1f) * Math.max(0.02f, illumination);
		int detail = m.detail.get().ordinal();
		GpuTextureView texture = resolveTexture(m.texture.get());
		float textureOpacity = textureValid ? m.textureOpacity.getFloat() : 0f;
		int planets = m.planets.getInt();
		float time = (System.nanoTime() - startNanos) / 1e9f;

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView view = encoder.mapBuffer(uniforms.currentBuffer(), false, true)) {
			Std140Builder b = Std140Builder.intoBuffer(view.data());
			b.putMat4f(invViewProj);
			putColor(b, zenith, brightness);
			putColor(b, horizon, m.gradient.getFloat());
			putColor(b, ground, m.intensity.getFloat());
			putColor(b, m.horizonGlow.argb(), glowStrength);
			b.putVec4(sunDir.x, sunDir.y, sunDir.z, radius(m.sunSize.getFloat()));
			putColor(b, m.sunColor.argb(), m.sunGlow.getFloat());
			b.putVec4(moonDir.x, moonDir.y, moonDir.z, radius(m.moonSize.getFloat()));
			putColor(b, m.moonColor.argb(), m.moonGlow.getFloat());
			b.putVec4(style(m.sunStyle.get()), style(m.moonStyle.get()), phase, visibility);
			b.putVec4(m.starDensity.getFloat(), m.starSize.getFloat(), m.starBrightness.getFloat(), m.twinkle.getFloat());
			putColor(b, m.starColor.argb(), m.starVariance.getFloat());
			putColor(b, m.nebulaA.argb(), m.nebula.get() ? m.nebulaIntensity.getFloat() : 0f);
			putColor(b, m.nebulaB.argb(), m.nebulaScale.getFloat());
			b.putVec4(PLANET_A.x, PLANET_A.y, PLANET_A.z, planets >= 1 ? radius(m.planetASize.getFloat()) : 0f);
			putColor(b, m.planetAColor.argb(), m.planetARing.get() ? 1f : 0f);
			b.putVec4(PLANET_B.x, PLANET_B.y, PLANET_B.z, planets >= 2 ? radius(m.planetBSize.getFloat()) : 0f);
			putColor(b, m.planetBColor.argb(), m.planetBRing.get() ? 1f : 0f);
			b.putVec4((float) Math.toRadians(m.ringRadius.get()), m.ringOrnaments.getFloat(), 0.0022f, m.ring.get() ? 1f : 0f);
			putColor(b, m.ringColor.argb(), 1f);
			putColor(b, m.silhouetteColor.argb(), m.silhouettes.get().ordinal());
			b.putVec4(time, starAngle, detail, textureOpacity);
		}
		var target = Minecraft.getInstance().getMainRenderTarget();
		try (RenderPass pass = encoder.createRenderPass(() -> "Holo sky", target.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(HollowPipelines.HOLO_SKY);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("HoloSky", uniforms.currentBuffer());
			pass.bindTexture("SkyTexture", texture, RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
			pass.draw(0, 3);
		}
		uniforms.rotate();
	}

	private static float style(BodyStyle style) {
		return switch (style) {
			case HIDDEN -> 0f;
			case DISC -> 1f;
			case HALO -> 2f;
			case CRESCENT -> 3f;
			case ECLIPSE -> 4f;
		};
	}

	private static float radius(float diameterDegrees) {
		return (float) Math.toRadians(diameterDegrees / 2.0);
	}

	private static void putColor(Std140Builder b, int argb, float w) {
		b.putVec4(Colors.red(argb) / 255f, Colors.green(argb) / 255f, Colors.blue(argb) / 255f, w);
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
		return t * t * (3 - 2 * t);
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}

	/**
	 * Resolves the texture layer. Missing, unreadable or oversized images fall back to a blank
	 * texture, and the reason is logged once.
	 */
	private GpuTextureView resolveTexture(String idText) {
		if (idText.equals(resolvedTextureId) && resolvedGeneration == resourceGeneration && textureView != null) {
			return textureView;
		}
		resolvedTextureId = idText;
		resolvedGeneration = resourceGeneration;
		textureValid = false;
		textureView = blankView();
		if (idText.isEmpty()) {
			return textureView;
		}
		Identifier id = Identifier.tryParse(idText);
		Minecraft minecraft = Minecraft.getInstance();
		if (id == null || minecraft.getResourceManager().getResource(id).isEmpty()) {
			warn("Sky texture " + idText + " was not found in any resource pack; using no texture");
			return textureView;
		}
		try {
			AbstractTexture texture = minecraft.getTextureManager().getTexture(id);
			GpuTexture gpu = texture.getTexture();
			int w = gpu.getWidth(0);
			int h = gpu.getHeight(0);
			if (w < 2 || h < 2 || w > 8192 || h > 8192) {
				warn("Sky texture " + idText + " has unsupported size " + w + "x" + h + "; using no texture");
				return textureView;
			}
			textureView = texture.getTextureView();
			textureValid = true;
		} catch (RuntimeException e) {
			warn("Sky texture " + idText + " could not be loaded (" + e.getMessage() + "); using no texture");
		}
		return textureView;
	}

	private GpuTextureView blankView() {
		if (blank == null) {
			NativeImage image = new NativeImage(1, 1, false);
			image.setPixel(0, 0, 0);
			blank = new DynamicTexture(() -> "Holo sky blank", image);
			Minecraft.getInstance().getTextureManager().register(BLANK_TEXTURE, blank);
		}
		return blank.getTextureView();
	}

	private void warn(String message) {
		if (!message.equals(lastWarning)) {
			lastWarning = message;
			Hollow.LOGGER.warn(message);
			module().message(message);
		}
	}

	// ---------------------------------------------------------------- fog and clouds

	/** Blends atmospheric fog toward the current horizon color when the Holo sky is active. */
	public Vector4f tintFog(Vector4f fog) {
		SkyStudioModule m = module();
		float amount = m.fogTint.getFloat();
		if (amount <= 0f || !shouldReplaceWorldSky()) {
			return fog;
		}
		return new Vector4f(
			fog.x + (horizonNow.x * m.brightness.getFloat() - fog.x) * amount,
			fog.y + (horizonNow.y * m.brightness.getFloat() - fog.y) * amount,
			fog.z + (horizonNow.z * m.brightness.getFloat() - fog.z) * amount,
			fog.w);
	}

	public boolean hideClouds() {
		return shouldReplaceWorldSky() && module().cloudMode.get() == CloudMode.HIDDEN;
	}

	public int tintClouds(int color) {
		if (!shouldReplaceWorldSky() || module().cloudMode.get() != CloudMode.TINTED) {
			return color;
		}
		int tint = module().cloudColor.argb();
		return Colors.argb(Colors.alpha(color),
			Colors.red(color) * Colors.red(tint) / 255,
			Colors.green(color) * Colors.green(tint) / 255,
			Colors.blue(color) * Colors.blue(tint) / 255);
	}

	/** Frees GPU buffers; called when Sky Studio is disabled. */
	public void release() {
		if (uniforms != null) {
			RenderSystem.assertOnRenderThread();
			uniforms.close();
			uniforms = null;
		}
		replacingThisFrame = false;
	}
}
