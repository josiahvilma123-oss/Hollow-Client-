package dev.hollow.character;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix3x2f;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/**
 * Draws a 3D character in menus: the player's own skin wearing a Character Studio outfit, with
 * a gentle idle animation. Uses Minecraft's picture-in-picture GUI rendering (the same path as
 * the skin preview in the options), so it has proper depth and lighting.
 */
public final class CharacterView {
	private static final float PIVOT_Y = -1.0625F;

	private static EntityModelSet bakedFrom;
	private static PlayerModel wide;
	private static PlayerModel slim;
	private static final Map<Cosmetic, PlayerModel> OUTFITS = new EnumMap<>(Cosmetic.class);
	private static Supplier<PlayerSkin> skin;

	private CharacterView() {
	}

	/** Registers the renderer with Fabric API. Must run during client initialisation. */
	public static void register() {
		SpecialGuiElementRegistry.register(context -> new Renderer(context.vertexConsumers()));
	}

	/**
	 * Queues a character in the given screen box.
	 *
	 * @param yaw       turn of the whole figure in degrees (0 faces the viewer)
	 * @param lookYaw   extra head turn in degrees, e.g. towards the mouse
	 * @param motion    idle animation intensity; 0 holds the pose still
	 */
	public static void draw(GuiGraphics g, Cosmetic cosmetic, int x0, int y0, int x1, int y1, float yaw, float pitch, float lookYaw,
			float lookPitch, float time, float motion) {
		// The figure is drawn into its own texture and placed in screen space, so map the box
		// through the current GUI transform (scaled previews, window open animations).
		Matrix3x2f pose = g.pose();
		Vector2f a = pose.transformPosition(x0, y0, new Vector2f());
		Vector2f b = pose.transformPosition(x1, y1, new Vector2f());
		int sx0 = Math.round(Math.min(a.x, b.x));
		int sy0 = Math.round(Math.min(a.y, b.y));
		int sx1 = Math.round(Math.max(a.x, b.x));
		int sy1 = Math.round(Math.max(a.y, b.y));
		if (sx1 - sx0 < 4 || sy1 - sy0 < 8) {
			return;
		}
		// Frame the figure by its outfit's height so hats and horns stay inside the box.
		float scale = 0.97F * (sy1 - sy0) / cosmetic.viewHeight();
		ScreenRectangle scissor = g.scissorStack.peek();
		g.guiRenderState.submitPicturesInPictureState(new State(cosmetic, yaw, pitch, lookYaw, lookPitch, time, motion, sx0, sy0, sx1, sy1, scale,
			scissor, PictureInPictureRenderState.getBounds(sx0, sy0, sx1, sy1, scissor)));
	}

	/** Box width for a figure of the given height, with room for scythes, wings and hat brims when it turns. */
	public static int widthFor(int height) {
		return Math.round(height * 0.8F);
	}

	public record State(Cosmetic cosmetic, float yaw, float pitch, float lookYaw, float lookPitch, float time, float motion, int x0, int y0, int x1,
			int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
	}

	private static void ensureModels() {
		EntityModelSet models = Minecraft.getInstance().getEntityModels();
		if (models != bakedFrom) {
			bakedFrom = models;
			wide = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER), false);
			slim = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
			OUTFITS.clear();
			for (Cosmetic cosmetic : Cosmetic.values()) {
				if (cosmetic.layer() != null) {
					OUTFITS.put(cosmetic, new PlayerModel(models.bakeLayer(cosmetic.layer()), false));
				}
			}
		}
		if (skin == null) {
			Minecraft minecraft = Minecraft.getInstance();
			// Starts with the default skin and switches to the real one once it has downloaded.
			skin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
		}
	}

	/** Idle pose: breathing arms, a slight sway and the head following the given look angles. */
	private static void pose(PlayerModel model, State state) {
		model.resetPose();
		float t = state.time();
		float m = state.motion();
		float breathe = (float) Math.sin(t * 1.6F);
		model.head.yRot = (float) Math.toRadians(state.lookYaw());
		model.head.xRot = (float) Math.toRadians(state.lookPitch());
		model.rightArm.zRot = 0.07F + 0.035F * breathe * m;
		model.leftArm.zRot = -0.07F - 0.035F * breathe * m;
		model.rightArm.xRot = (float) Math.sin(t * 0.9F) * 0.07F * m;
		model.leftArm.xRot = -(float) Math.sin(t * 0.9F) * 0.07F * m;
		model.body.yRot = (float) Math.sin(t * 0.5F) * 0.03F * m;
	}

	private static void copyPose(PlayerModel from, PlayerModel to) {
		to.resetPose();
		to.head.loadPose(from.head.storePose());
		to.body.loadPose(from.body.storePose());
		to.rightArm.loadPose(from.rightArm.storePose());
		to.leftArm.loadPose(from.leftArm.storePose());
		to.rightLeg.loadPose(from.rightLeg.storePose());
		to.leftLeg.loadPose(from.leftLeg.storePose());
	}

	private static final class Renderer extends PictureInPictureRenderer<State> {
		Renderer(MultiBufferSource.BufferSource bufferSource) {
			super(bufferSource);
		}

		@Override
		public Class<State> getRenderStateClass() {
			return State.class;
		}

		@Override
		protected void renderToTexture(State state, PoseStack poseStack) {
			ensureModels();
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.PLAYER_SKIN);
			int guiScale = minecraft.getWindow().getGuiScale();
			Matrix4fStack modelView = RenderSystem.getModelViewStack();
			modelView.pushMatrix();
			float pixels = state.scale() * guiScale;
			modelView.rotateAround(Axis.XP.rotationDegrees(state.pitch()), 0.0F, pixels * -PIVOT_Y, 0.0F);
			poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw()));
			poseStack.translate(0.0F, -1.6010001F, 0.0F);

			PlayerSkin playerSkin = skin.get();
			PlayerModel body = playerSkin.model() == PlayerModelType.SLIM ? slim : wide;
			pose(body, state);
			body.renderToBuffer(poseStack, bufferSource.getBuffer(body.renderType(playerSkin.body().texturePath())), LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY);
			PlayerModel outfit = OUTFITS.get(state.cosmetic());
			if (outfit != null) {
				copyPose(body, outfit);
				outfit.renderToBuffer(poseStack, bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(state.cosmetic().texture())),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}
			bufferSource.endBatch();
			modelView.popMatrix();
		}

		@Override
		protected String getTextureLabel() {
			return "holo character";
		}
	}
}
