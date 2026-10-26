package dev.hollow.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.hollow.Hollow;
import dev.hollow.compat.ShaderPackCompat;
import dev.hollow.core.anim.Smooth;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.render.HollowPipelines;
import java.util.OptionalInt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;

/**
 * Camera motion blur by exponential frame accumulation. Runs once per frame after the world
 * and hand are drawn but before the HUD, so text, crosshair and menus are never blurred.
 * When the camera is still the pass is skipped entirely and only the history is refreshed.
 */
public final class MotionBlurRenderer {
	public static final MotionBlurRenderer INSTANCE = new MotionBlurRenderer();
	private static final int UBO_SIZE = new Std140SizeCalculator().putFloat().get();

	private TextureTarget history;
	private TextureTarget scratch;
	private MappableRingBuffer config;
	private boolean historyValid;
	private long lastNanos = -1;
	private float lastYaw;
	private float lastPitch;
	private double motion;
	private boolean hookReached;

	private MotionBlurRenderer() {
	}

	public boolean hookReached() {
		return hookReached;
	}

	/** Called from the GameRenderer hook with the main render target holding the finished world image. */
	public void onWorldRendered(Minecraft minecraft) {
		hookReached = true;
		MotionBlurModule module = Hollow.module(MotionBlurModule.class);
		if (!module.cameraActive() || minecraft.level == null
				|| module.pauseWithShaders.get() && ShaderPackCompat.shaderPackActive()) {
			if (history != null) {
				release();
			}
			lastNanos = -1;
			return;
		}
		if (!HollowPipelines.usable(HollowPipelines.MOTION_BLUR)) {
			module.markUnavailable("This graphics driver couldn't compile the motion blur shader");
			release();
			return;
		}
		try {
			process(minecraft, module);
		} catch (RuntimeException e) {
			Hollow.LOGGER.error("Motion blur failed; turning camera blur off for this session", e);
			module.camera.set(false);
			release();
		}
	}

	private void process(Minecraft minecraft, MotionBlurModule module) {
		Camera camera = minecraft.gameRenderer.getMainCamera();
		long now = System.nanoTime();
		double delta = lastNanos < 0 ? 0 : (now - lastNanos) / 1e9;
		float yaw = camera.yRot();
		float pitch = camera.xRot();
		double speed = lastNanos < 0 ? 0 : MotionBlurMath.angularSpeed(yaw - lastYaw, pitch - lastPitch, delta);
		lastNanos = now;
		lastYaw = yaw;
		lastPitch = pitch;

		// Rise quickly when a turn starts, then fade out over the configured duration.
		double target = MotionBlurMath.motionFactor(speed, module.sensitivity.get());
		double tau = target > motion ? 0.02 : Math.max(0.02, module.duration.get() / 1000.0);
		motion = Smooth.approach(motion, target, delta, tau);

		RenderTarget main = minecraft.getMainRenderTarget();
		ensureTargets(main.width, main.height);
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		double blend = MotionBlurMath.blendFactor(motion, module.intensity.get(), module.duration.get(), delta);
		if (!historyValid || blend < 0.004) {
			encoder.copyTextureToTexture(main.getColorTexture(), history.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
			historyValid = true;
			return;
		}

		try (GpuBuffer.MappedView view = encoder.mapBuffer(config.currentBuffer(), false, true)) {
			Std140Builder.intoBuffer(view.data()).putFloat((float) blend);
		}
		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		try (RenderPass pass = encoder.createRenderPass(() -> "Hollow motion blur", scratch.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(HollowPipelines.MOTION_BLUR);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("MotionBlurConfig", config.currentBuffer());
			pass.bindTexture("InSampler", main.getColorTextureView(), sampler);
			pass.bindTexture("PrevSampler", history.getColorTextureView(), sampler);
			pass.draw(0, 3);
		}
		config.rotate();
		encoder.copyTextureToTexture(scratch.getColorTexture(), main.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
		encoder.copyTextureToTexture(scratch.getColorTexture(), history.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
	}

	private void ensureTargets(int width, int height) {
		if (history == null) {
			history = new TextureTarget("Hollow motion blur history", width, height, false);
			scratch = new TextureTarget("Hollow motion blur scratch", width, height, false);
			config = new MappableRingBuffer(() -> "Hollow motion blur config", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UBO_SIZE);
			historyValid = false;
		} else if (history.width != width || history.height != height) {
			history.resize(width, height);
			scratch.resize(width, height);
			historyValid = false;
		}
	}

	/** Frees GPU resources; called when the effect is turned off so nothing lingers. */
	public void release() {
		if (history != null) {
			RenderSystem.assertOnRenderThread();
			history.destroyBuffers();
			scratch.destroyBuffers();
			config.close();
			history = null;
			scratch = null;
			config = null;
		}
		historyValid = false;
		motion = 0;
	}
}
