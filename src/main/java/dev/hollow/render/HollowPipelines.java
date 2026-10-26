package dev.hollow.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.hollow.Hollow;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Holo's render pipelines. They are deliberately <em>not</em> registered as static pipelines:
 * Minecraft treats every static pipeline as required, so one shader that a graphics driver
 * refuses to compile would fail the whole resource load and leave a black screen. Instead they
 * compile on first use, and callers check {@link #usable} so a driver problem only turns off
 * the effect that needs that shader.
 */
public final class HollowPipelines {
	private static final RenderPipeline.Snippet LINES_NO_FOG = RenderPipeline.builder(
			RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
		.withVertexShader(id("core/hollow_lines"))
		.withFragmentShader(id("core/hollow_lines"))
		.withBlend(BlendFunction.TRANSLUCENT)
		.withCull(false)
		.withDepthWrite(false)
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
		.buildSnippet();

	public static final RenderPipeline LINES_SEE_THROUGH = RenderPipeline.builder(LINES_NO_FOG)
		.withLocation(id("pipeline/lines_see_through"))
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.build();

	public static final RenderPipeline LINES_DEPTH = RenderPipeline.builder(LINES_NO_FOG)
		.withLocation(id("pipeline/lines_depth"))
		.build();

	public static final RenderPipeline FILL_SEE_THROUGH = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
		.withLocation(id("pipeline/fill_see_through"))
		.withCull(false)
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.build();

	public static final RenderPipeline FILL_DEPTH = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
		.withLocation(id("pipeline/fill_depth"))
		.withCull(false)
		.build();

	/** Additive, depth-ignoring ribbons for Hollow Hands swing trails. */
	public static final RenderPipeline HAND_TRAIL = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
		.withLocation(id("pipeline/hand_trail"))
		.withCull(false)
		.withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.build();

	public static final RenderPipeline MOTION_BLUR = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(id("pipeline/motion_blur"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(id("post/motion_blur"))
		.withSampler("InSampler")
		.withSampler("PrevSampler")
		.withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build();

	/** Holo Sky Studio: the whole sky in one full-screen procedural pass. */
	public static final RenderPipeline HOLO_SKY = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(id("pipeline/holo_sky"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(id("sky/holo_sky"))
		.withSampler("SkyTexture")
		.withUniform("HoloSky", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build();

	public static final RenderType LINES_SEE_THROUGH_TYPE = RenderType.create("hollow_lines_see_through",
		RenderSetup.builder(LINES_SEE_THROUGH).createRenderSetup());
	public static final RenderType LINES_DEPTH_TYPE = RenderType.create("hollow_lines_depth",
		RenderSetup.builder(LINES_DEPTH).createRenderSetup());
	public static final RenderType FILL_SEE_THROUGH_TYPE = RenderType.create("hollow_fill_see_through",
		RenderSetup.builder(FILL_SEE_THROUGH).sortOnUpload().createRenderSetup());
	public static final RenderType FILL_DEPTH_TYPE = RenderType.create("hollow_fill_depth",
		RenderSetup.builder(FILL_DEPTH).sortOnUpload().createRenderSetup());
	public static final RenderType HAND_TRAIL_TYPE = RenderType.create("hollow_hand_trail",
		RenderSetup.builder(HAND_TRAIL).createRenderSetup());

	private static final Set<RenderPipeline> REPORTED = ConcurrentHashMap.newKeySet();

	private HollowPipelines() {
	}

	/** Forces class initialisation early, so a problem in a pipeline definition shows up at start-up. */
	public static void bootstrap() {
		Hollow.LOGGER.debug("Created Holo render pipelines");
	}

	/**
	 * Whether the pipeline's shaders compiled on this graphics driver. Compiles on first call
	 * (and again after each resource reload); must be called on the render thread.
	 */
	public static boolean usable(RenderPipeline pipeline) {
		boolean valid = RenderSystem.getDevice().precompilePipeline(pipeline).isValid();
		if (!valid && REPORTED.add(pipeline)) {
			Hollow.LOGGER.error("Holo shader pipeline {} could not be compiled by this graphics driver (see the error above); "
				+ "the effect that uses it is turned off", pipeline.getLocation());
		}
		return valid;
	}

	public static RenderType lines(boolean seeThrough) {
		return seeThrough ? LINES_SEE_THROUGH_TYPE : LINES_DEPTH_TYPE;
	}

	public static RenderType fill(boolean seeThrough) {
		return seeThrough ? FILL_SEE_THROUGH_TYPE : FILL_DEPTH_TYPE;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Hollow.MOD_ID, path);
	}
}
